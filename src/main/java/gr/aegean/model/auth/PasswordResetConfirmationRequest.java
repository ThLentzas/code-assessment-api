package gr.aegean.model.auth;

import jakarta.validation.constraints.NotBlank;


public record PasswordResetConfirmationRequest(
        String token,
        @NotBlank(message = "The Password field is required.")
        String newPassword) {
}
