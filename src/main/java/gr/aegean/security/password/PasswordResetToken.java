package gr.aegean.security.password;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PasswordResetToken {
    private Integer userId;
    private String resetToken;
    private LocalDateTime expiryDate;

    public PasswordResetToken(Integer userId, LocalDateTime expiryDate) {
        this.userId = userId;
        this.expiryDate = expiryDate;
    }
}