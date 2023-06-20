package gr.aegean.security.auth;

public record PasswordResetRequest(String email, String username) {
}
