package gr.aegean.model.dto.user;

import jakarta.validation.constraints.NotBlank;


public record UserPasswordUpdateRequest(
        @NotBlank(message = "Old password is required")
        String oldPassword,
        /*
            New is allowed here as a prefix. In Java is a reserved keyword and typically should be avoided as a prefix.
         */
        @NotBlank(message = "New password is required")
        String newPassword) {
}
