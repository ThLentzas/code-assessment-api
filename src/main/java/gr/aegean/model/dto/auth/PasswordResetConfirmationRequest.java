package gr.aegean.model.dto.auth;

import jakarta.validation.constraints.NotBlank;


public record PasswordResetConfirmationRequest(
        @NotBlank(message = "No token provided")
        String token,
        @NotBlank(message = "The Password field is required")
        String password) {
}
