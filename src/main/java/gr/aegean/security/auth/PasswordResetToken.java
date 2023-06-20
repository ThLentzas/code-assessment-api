package gr.aegean.security.auth;

import java.time.LocalDateTime;

public record PasswordResetToken(Integer userId, String resetToken, LocalDateTime expiryDate) {}