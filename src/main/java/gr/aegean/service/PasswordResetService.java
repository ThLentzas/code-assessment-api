package gr.aegean.service;

import gr.aegean.exception.BadCredentialsException;
import gr.aegean.model.token.PasswordResetToken;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.model.passwordreset.PasswordResetConfirmationRequest;
import gr.aegean.model.passwordreset.PasswordResetRequest;
import gr.aegean.model.passwordreset.PasswordResetResult;
import gr.aegean.utility.StringUtils;

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

    public PasswordResetResult createPasswordResetToken(PasswordResetRequest resetRequest) {
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
                    passwordResetRepository.createToken(passwordResetToken);

                    emailService.sendPasswordResetRequestEmail(resetRequest.email(), token);
                });

        return new PasswordResetResult("If your email address exists in our database, you will receive a password " +
                "recovery link at your email address in a few minutes.");
    }

    public void validatePasswordResetToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException("Reset password token is invalid");
        }

        String hashedToken = StringUtils.hashToken(token);

        passwordResetRepository.findToken(hashedToken)
                .ifPresentOrElse(passwordResetToken -> {
                    if (passwordResetToken.expiryDate().isBefore(LocalDateTime.now())) {
                        throw new BadCredentialsException("The password reset link has expired. " +
                                "Please request a new one.");
                    }}, () -> {
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
