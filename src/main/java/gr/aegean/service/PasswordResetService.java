package gr.aegean.service;

import gr.aegean.exception.BadCredentialsException;
import gr.aegean.model.user.User;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.security.password.PasswordResetConfirmationRequest;
import gr.aegean.security.password.PasswordResetRequest;
import gr.aegean.security.password.PasswordResetResult;
import gr.aegean.security.password.PasswordResetToken;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import gr.aegean.utility.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final EmailService emailService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetResult createPasswordResetToken(PasswordResetRequest passwordResetRequest) {
        validatePasswordResetRequest(passwordResetRequest);

        Optional<User> optionalUser = userRepository.findUserByEmail(passwordResetRequest.email());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            String token = generateToken();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(2);
            String hashedToken = StringUtils.hashToken(token);
            PasswordResetToken passwordResetToken = new PasswordResetToken(user.getId(), hashedToken, expiryDate);
            passwordResetRepository.createPasswordResetToken(passwordResetToken);

            emailService.sendPasswordResetLinkEmail(passwordResetRequest.email(), token);
        }

        return new PasswordResetResult("If your email address exists in our database, you will receive a password " +
                "recovery link at your email address in a few minutes.");
    }

    public void validatePasswordResetToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException("Reset password token is invalid");
        }

        String hashedToken = StringUtils.hashToken(token);
        Optional<PasswordResetToken> optionalPasswordResetToken = passwordResetRepository
                .findPasswordResetToken(hashedToken);
        PasswordResetToken passwordResetToken = optionalPasswordResetToken
                .orElseThrow(() -> new BadCredentialsException("Reset password token is invalid"));

        if (passwordResetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("The password reset link has expired. Please request a new one.");
        }
    }

    public void resetPassword(PasswordResetConfirmationRequest resetConfirmationRequest) {
        //validate the token
        validatePasswordResetToken(resetConfirmationRequest.token());

        //validate/hash the updated password
        userService.validatePassword(resetConfirmationRequest.updatedPassword());
        String hashedPassword = passwordEncoder.encode(resetConfirmationRequest.updatedPassword());

        //update the password in db and delete the password reset token record after
        String hashedToken = StringUtils.hashToken(resetConfirmationRequest.token());
        Optional<PasswordResetToken> optionalPasswordResetToken = passwordResetRepository
                .findPasswordResetToken(hashedToken);

        if (optionalPasswordResetToken.isPresent()) {
            PasswordResetToken passwordResetToken = optionalPasswordResetToken.get();

            userRepository.updatePassword(passwordResetToken.getUserId(), hashedPassword);
            passwordResetRepository.deletePasswordResetToken(hashedToken);

            //send confirmation email
            sendConfirmationEmail(passwordResetToken.getUserId());
        }
    }

    private void sendConfirmationEmail(Integer userId) {
        Optional<User> optionalUser = userRepository.findUserByUserId(userId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            emailService.sendPasswordResetConfirmationEmail(user.getEmail(), user.getUsername());
        }
    }

    private String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[16];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void validatePasswordResetRequest(PasswordResetRequest passwordResetRequest) {
        if (passwordResetRequest.email() == null || passwordResetRequest.email().isEmpty()) {
            throw new BadCredentialsException("The Email field is required.");
        }
    }
}
