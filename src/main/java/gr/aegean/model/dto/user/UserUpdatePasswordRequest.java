package gr.aegean.model.dto.user;

import jakarta.validation.constraints.NotBlank;


public record UserUpdatePasswordRequest(
        @NotBlank(message = "Old password is required")
        String oldPassword,
        @NotBlank(message = "New password is required")
        String updatedPassword) {

}
