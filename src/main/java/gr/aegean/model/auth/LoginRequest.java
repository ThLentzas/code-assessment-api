package gr.aegean.model.auth;

import jakarta.validation.constraints.NotBlank;


public record LoginRequest(
        @NotBlank(message = "All fields are necessary.")
        String email,
        @NotBlank(message = "All fields are necessary.")
        String password) {
}
