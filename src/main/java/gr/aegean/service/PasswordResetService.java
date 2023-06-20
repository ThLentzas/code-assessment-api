package gr.aegean.service;

import gr.aegean.exception.BadCredentialsException;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.security.auth.PasswordResetRequest;
import gr.aegean.security.auth.PasswordResetToken;
import gr.aegean.model.user.User;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;

    public void createPasswordResetRequest(PasswordResetRequest passwordResetRequest) {
        User user = userRepository.findUserByEmail(passwordResetRequest.email());
        if(!user.getUsername().equals(passwordResetRequest.username())) {
            throw new BadCredentialsException("");
        }

        String token = generateToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(2);
        String hashedToken = hashToken(token);
        PasswordResetToken passwordResetToken = new PasswordResetToken(user.getId(), hashedToken, expiryDate);

        passwordResetRepository.createPasswordResetRequest(passwordResetToken);

        emailService.sendPasswordResetEmail(passwordResetRequest.username(), token);
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
}
