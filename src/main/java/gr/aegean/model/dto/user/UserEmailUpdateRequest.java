package gr.aegean.model.dto.user;

import jakarta.validation.constraints.NotBlank;


public record UserEmailUpdateRequest(
        @NotBlank(message = "The Email field is required")
        String email,
        @NotBlank(message = "The Password field is required")
        String password) {
}