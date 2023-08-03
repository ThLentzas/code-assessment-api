package gr.aegean.model.user;

import jakarta.validation.constraints.NotBlank;


public record UserUpdateEmailRequest(
        @NotBlank(message = "The email field is required")
        String email,
        @NotBlank(message = "The password field is required")
        String password) {

}