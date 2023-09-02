package gr.aegean.entity;

import java.time.LocalDateTime;

public record PasswordResetToken(Integer userId, String token, LocalDateTime expiryDate) {
}