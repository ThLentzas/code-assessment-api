package gr.aegean.service.auth;

import gr.aegean.entity.PasswordResetToken;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.model.dto.auth.PasswordResetConfirmationRequest;
import gr.aegean.model.dto.auth.PasswordResetRequest;
import gr.aegean.service.email.EmailService;
import gr.aegean.utility.PasswordValidator;
import gr.aegean.utility.StringUtils;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final EmailService emailService;
    private final PasswordResetRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void createPasswordResetToken(PasswordResetRequest resetRequest) {
        userRepository.findUserByEmail(resetRequest.email())
                .ifPresent(user -> {
                    /*
                        The generated token will be on the email link and the hashed version will be stored in our db.
                     */
                    String token = StringUtils.generateToken();
                    String hashedToken = StringUtils.hashToken(token);
                    LocalDateTime expiryDate = LocalDateTime.now().plusHours(3);
                    PasswordResetToken passwordResetToken = new PasswordResetToken(
                            user.getId(),
                            hashedToken,
                            expiryDate
                    );

                    /*
                        If the user requested a new password reset without clicking the link on the previous email, we
                        have to invalidate the previous generated tokens. Best approach would also include rate limiting
                     */
                    passwordResetRepository.deleteAllUserTokens(user.getId());
                    passwordResetRepository.saveToken(passwordResetToken);

                    emailService.sendPasswordResetEmail(resetRequest.email(), token);
                });
    }

    public void resetPassword(PasswordResetConfirmationRequest resetConfirmationRequest) {
        //Validate the token
        validatePasswordResetToken(resetConfirmationRequest.token());

        //validate/hash the updated password
        PasswordValidator.validatePassword(resetConfirmationRequest.password());
        String hashedPassword = passwordEncoder.encode(resetConfirmationRequest.password());

        //Update the password in db and delete the password reset token record after so subsequent requests will fail
        String hashedToken = StringUtils.hashToken(resetConfirmationRequest.token());

        passwordResetRepository.findToken(hashedToken)
                .ifPresent(passwordResetToken -> {
                    userRepository.updatePassword(passwordResetToken.userId(), hashedPassword);
                    passwordResetRepository.deleteToken(hashedToken);

                    // send confirmation email
                    userRepository.findUserByUserId(passwordResetToken.userId())
                            .ifPresent(user -> emailService.sendPasswordResetSuccessEmail(
                                    user.getEmail(),
                                    user.getUsername()));
                });
    }

    private void validatePasswordResetToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Reset password token is invalid");
        }

        String hashedToken = StringUtils.hashToken(token);

        passwordResetRepository.findToken(hashedToken)
                .ifPresentOrElse(passwordResetToken -> {
                    if (passwordResetToken.expiryDate().isBefore(LocalDateTime.now())) {
                        throw new BadCredentialsException("The password reset link has expired. " +
                                "Please request a new one");
                    }
                }, () -> {
                    throw new BadCredentialsException("Reset password token is invalid");
                });
    }
}
