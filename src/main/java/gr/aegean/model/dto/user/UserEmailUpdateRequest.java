package gr.aegean.model.dto.user;

import jakarta.validation.constraints.NotBlank;


public record UserEmailUpdateRequest(
        @NotBlank(message = "The email field is required")
        String email,
        @NotBlank(message = "The password field is required")
        String password) {

}