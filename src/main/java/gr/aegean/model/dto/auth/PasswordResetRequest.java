package gr.aegean.model.dto.auth;

import jakarta.validation.constraints.NotBlank;


public record PasswordResetRequest(@NotBlank(message = "The Email field is required.")
                                   String email) {
}
