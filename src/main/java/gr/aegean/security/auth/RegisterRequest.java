package gr.aegean.security.auth;

public record RegisterRequest(
        String firstname,
        String lastname,
        String username,
        String email,
        String password,
        String bio,
        String location,
        String company) {}

