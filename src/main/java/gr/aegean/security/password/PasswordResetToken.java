package gr.aegean.security.password;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@AllArgsConstructor
public class PasswordResetToken {
    private Integer userId;
    private String resetToken;
    private LocalDateTime expiryDate;

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(obj == null) {
            return false;
        }

        if(obj instanceof PasswordResetToken tokeObj) {
            return userId.equals(tokeObj.userId)
                    && resetToken.equals(tokeObj.resetToken);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, resetToken, expiryDate);
    }



}