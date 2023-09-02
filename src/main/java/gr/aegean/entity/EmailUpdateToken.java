package gr.aegean.entity;

import java.time.LocalDateTime;


public record EmailUpdateToken(Integer userId, String token, String email, LocalDateTime expiryDate) {
}