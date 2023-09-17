package gr.aegean.model.dto.auth;

import jakarta.validation.constraints.NotBlank;


public record RegisterRequest(
        @NotBlank(message = "The First Name field is required")
        String firstname,
        @NotBlank(message = "The Last Name field is required")
        String lastname,
        @NotBlank(message = "The Username field is required")
        String username,
        @NotBlank(message = "The Email field is required")
        String email,
        @NotBlank(message = "The Password field is required")
        String password) {
}

