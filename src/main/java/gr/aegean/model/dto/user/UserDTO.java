package gr.aegean.model.dto.user;


public record UserDTO(
        Integer id,
        String firstname,
        String lastname,
        String username,
        String email,
        String bio,
        String location,
        String company) {
}
