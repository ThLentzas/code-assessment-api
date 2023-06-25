package gr.aegean.model.passwordreset;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @NotBlank(message = "The Email field is required.")
        String email) {}
