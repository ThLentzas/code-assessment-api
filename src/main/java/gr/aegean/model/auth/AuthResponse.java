package gr.aegean.model.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private Integer id;
    public AuthResponse(String token) {
        this.token = token;
    }
}
