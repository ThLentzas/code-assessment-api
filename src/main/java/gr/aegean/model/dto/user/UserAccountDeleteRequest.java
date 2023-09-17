package gr.aegean.model.dto.user;

import jakarta.validation.constraints.NotBlank;


public record UserAccountDeleteRequest(@NotBlank(message = "The Password field is required")
                                       String password) {
}
