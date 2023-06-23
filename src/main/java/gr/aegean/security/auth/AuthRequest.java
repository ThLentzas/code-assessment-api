package gr.aegean.security.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "All fields are necessary.")
        String email,
        @NotBlank(message = "All fields are necessary.")
        String password) {}
