package gr.aegean.model.token;

import java.time.LocalDateTime;
import java.util.Objects;

public record PasswordResetToken(Integer userId, String token, LocalDateTime expiryDate){
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
                    && token.equals(tokeObj.token);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, token);
    }
}