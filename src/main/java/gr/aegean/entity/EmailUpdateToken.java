package gr.aegean.entity;

import java.time.LocalDateTime;
import java.util.Objects;


public record EmailUpdateToken(Integer userId, String token, String email, LocalDateTime expiryDate) {

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof EmailUpdateToken tokenObj) {
            return userId.equals(tokenObj.userId)
                    && token.equals(tokenObj.token)
                    && email.equals(tokenObj.email);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, token, email);
    }
}