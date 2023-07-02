package gr.aegean.model.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Integer id;
    public AuthResponse(String token) {
        this.token = token;
    }
}
