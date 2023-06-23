package gr.aegean.model.user;

public record UserUpdateRequest(
        String firstname,
        String lastname,
        String username,
        String email,
        String password,
        String bio,
        String location,
        String company
) {}
