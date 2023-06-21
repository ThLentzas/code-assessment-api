package gr.aegean.security.password;

public record PasswordResetConfirmationRequest(String token, String updatedPassword) {}
