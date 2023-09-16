package gr.aegean.entity;

import java.time.LocalDateTime;

/*
    It has to be LocalDateTime and not Date because we are setting the expiry date 3 hours later, using Date we wouldn't
    have access to hours.
 */
public record EmailUpdateToken(Integer userId, String token, String email, LocalDateTime expiryDate) {
}