package gr.aegean.service.auth;

import gr.aegean.entity.PasswordResetToken;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.model.dto.auth.PasswordResetConfirmationRequest;
import gr.aegean.model.dto.auth.PasswordResetRequest;
import gr.aegean.service.email.EmailService;
import gr.aegean.utility.PasswordValidator;
import gr.aegean.utility.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final EmailService emailService;
    private final PasswordResetRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);

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

    /**
     * @return true if the token is valid and the provided password meets the requirements, false otherwise.
     */

    public boolean resetPassword(PasswordResetConfirmationRequest resetConfirmationRequest) {
        //Validate the password reset token
        if (isValidPasswordToken(resetConfirmationRequest.token())) {
            PasswordValidator.validatePassword(resetConfirmationRequest.password());
            String hashedPassword = passwordEncoder.encode(resetConfirmationRequest.password());

            /*
                Update the password in db and delete the password reset token record after so subsequent requests will
                fail
             */
            String hashedToken = StringUtils.hashToken(resetConfirmationRequest.token());
            passwordResetRepository.findToken(hashedToken)
                    .ifPresent(passwordResetToken -> {
                        userRepository.updatePassword(passwordResetToken.userId(), hashedPassword);
                        passwordResetRepository.deleteToken(hashedToken);

                        // send confirmation email
                        userRepository.findUserById(passwordResetToken.userId())
                                .ifPresent(user -> emailService.sendPasswordResetSuccessEmail(
                                        user.getEmail(),
                                        user.getUsername()));
                    });
            return true;
        }

        return false;
    }

    /*
        This method validates the token that's encoded in the verification link. Since we can't really throw exceptions
        we keep a state on the server for each invalid case by logging, and we return false for each one.
     */
    private boolean isValidPasswordToken(String token) {
        if (token.isBlank()) {
            LOG.warn("Received empty email update token");

            return false;
        }

        String hashedToken = StringUtils.hashToken(token);
        Optional<PasswordResetToken> optionalToken = passwordResetRepository.findToken(hashedToken);
        if (optionalToken.isEmpty()) {
            LOG.warn("Invalid password reset token received: {}", token);

            return false;
        }

        PasswordResetToken passwordResetToken = optionalToken.get();
        if (passwordResetToken.expiryDate().isBefore(LocalDateTime.now())) {
            LOG.warn("The password reset link has expired for user with id: {}", passwordResetToken.userId());

            return false;
        }

        return true;
    }
}