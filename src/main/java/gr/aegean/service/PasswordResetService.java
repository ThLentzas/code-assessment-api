package gr.aegean.service;

import gr.aegean.exception.BadCredentialsException;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.user.User;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.security.password.PasswordResetConfirmationRequest;
import gr.aegean.security.password.PasswordResetRequest;
import gr.aegean.security.password.PasswordResetResult;
import gr.aegean.security.password.PasswordResetToken;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.bouncycastle.util.encoders.Hex;
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

    public PasswordResetResult createPasswordResetRequest(PasswordResetRequest passwordResetRequest) {
        Optional<User> optionalUser = validatePasswordResetRequest(passwordResetRequest);

        if(optionalUser.isPresent()) {
            User user = optionalUser.get();

            String token = generateToken();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(2);
            String hashedToken = hashToken(token);
            PasswordResetToken passwordResetToken = new PasswordResetToken(user.getId(), hashedToken, expiryDate);
            passwordResetRepository.createPasswordResetRequest(passwordResetToken);

            emailService.sendPasswordResetLinkEmail(passwordResetRequest.username(), token);
        }

        return new PasswordResetResult("If your email address exists in our database, you will receive a password " +
                "recovery link at your email address in a few minutes.");
    }

    public PasswordResetToken validatePasswordResetToken(String token) {
        String hashedToken = hashToken(token);
        Optional<PasswordResetToken> optionalPasswordResetToken = passwordResetRepository
                .findPasswordResetToken(hashedToken);
        PasswordResetToken passwordResetToken = optionalPasswordResetToken
                .orElseThrow(() -> new BadCredentialsException("Reset password token is invalid"));

        if(passwordResetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("The password reset link has expired. Please request a new one.");
        }

        return passwordResetToken;
    }

    public void resetPassword(PasswordResetConfirmationRequest resetConfirmationRequest) {
        //validate the token
        PasswordResetToken passwordResetToken = validatePasswordResetToken(resetConfirmationRequest.token());

        //validate/hash the updated password
        userService.validatePassword(resetConfirmationRequest.updatedPassword());
        String hashedPassword = passwordEncoder.encode(resetConfirmationRequest.updatedPassword());

        //update the password in db and delete the password reset token record after
        userRepository.updatePassword(passwordResetToken.getUserId(), hashedPassword);
        String hashToken = hashToken(resetConfirmationRequest.token());
        passwordResetRepository.deletePasswordResetToken(hashToken);

        //send confirmation email
        Optional<User> optionalUser = userRepository.findUserByUserId(passwordResetToken.getUserId());

        if(optionalUser.isPresent()) {
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

    private String hashToken(String token) {
        byte[] hash;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException sae) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        return new String(Hex.encode(hash));
    }

    private Optional<User> validatePasswordResetRequest(PasswordResetRequest passwordResetRequest) {
        return userRepository.findUserByEmail(passwordResetRequest.email());
    }
}
