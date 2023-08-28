package gr.aegean.model.dto.user;


public record UserProfileUpdateRequest(
        String firstname,
        String lastname,
        String username,
        String bio,
        String location,
        String company
) {
}
