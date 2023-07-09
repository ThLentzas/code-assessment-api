package gr.aegean.model.token;

import java.time.LocalDateTime;
import java.util.Objects;


public record EmailUpdateToken(Integer userId, String token, String email, LocalDateTime expiryDate){
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(obj == null) {
            return false;
        }

        if(obj instanceof EmailUpdateToken tokeObj) {
            return userId.equals(tokeObj.userId)
                    && token.equals(tokeObj.token)
                    && email.equals(tokeObj.email);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, token, email);
    }
}