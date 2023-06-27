package gr.aegean.model.token;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@AllArgsConstructor
public class VerificationToken {
    private Integer userId;
    private String token;
    private LocalDateTime expiryDate;
    private TokenType type;

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(obj == null) {
            return false;
        }

        if(obj instanceof VerificationToken tokeObj) {
            return userId.equals(tokeObj.userId)
                    && token.equals(tokeObj.token)
                    && type.equals(tokeObj.type);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, token, type);
    }
}