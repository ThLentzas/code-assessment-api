package gr.aegean.service.auth;

import gr.aegean.entity.PasswordResetToken;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.model.auth.PasswordResetConfirmationRequest;
import gr.aegean.model.auth.PasswordResetRequest;
import gr.aegean.model.auth.PasswordResetResponse;
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

    public PasswordResetResponse createPasswordResetToken(PasswordResetRequest resetRequest) {
        userRepository.findUserByEmail(resetRequest.email())
                .ifPresent(user -> {
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

                    emailService.sendPasswordResetRequestEmail(resetRequest.email(), token);
                });

        /*
            We return a generic response for security reasons, no matter if the emails exists or not.
         */
        return new PasswordResetResponse("If your email address exists in our database, you will receive a password " +
                "recovery link at your email address in a few minutes.");
    }

    public void validatePasswordResetToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Reset password token is invalid");
        }

        String hashedToken = StringUtils.hashToken(token);

        passwordResetRepository.findToken(hashedToken)
                .ifPresentOrElse(passwordResetToken -> {
                    if (passwordResetToken.expiryDate().isBefore(LocalDateTime.now())) {
                        throw new BadCredentialsException("The password reset link has expired. " +
                                "Please request a new one.");
                    }
                }, () -> {
                    throw new BadCredentialsException("Reset password token is invalid");
                });
    }

    public void resetPassword(PasswordResetConfirmationRequest resetConfirmationRequest) {
        //validate the token
        validatePasswordResetToken(resetConfirmationRequest.token());

        //validate/hash the updated password
        StringUtils.validatePassword(resetConfirmationRequest.newPassword());
        String hashedPassword = passwordEncoder.encode(resetConfirmationRequest.newPassword());

        //update the password in db and delete the password reset token record after so subsequent requests will fail
        String hashedToken = StringUtils.hashToken(resetConfirmationRequest.token());

        passwordResetRepository.findToken(hashedToken)
                .ifPresent(passwordResetToken -> {
                    userRepository.updatePassword(passwordResetToken.userId(), hashedPassword);
                    passwordResetRepository.deleteToken(hashedToken);

                    // send confirmation email
                    sendConfirmationEmail(passwordResetToken.userId());
                });
    }

    private void sendConfirmationEmail(Integer userId) {
        userRepository.findUserByUserId(userId)
                .ifPresent(user -> emailService.sendPasswordResetConfirmationEmail(
                        user.getEmail(),
                        user.getUsername()));
    }
}
