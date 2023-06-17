package gr.aegean.model.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
    private Integer id;
    private String firstname;
    private String lastname;
    private String username;
    private String email;
    private String password;
    private String bio;
    private String location;
    private String company;

    public User(
            String firstname,
            String lastname,
            String username,
            String email,
            String password,
            String bio,
            String location,
            String company) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.username = username;
        this.email = email;
        this.password = password;
        this.bio = bio;
        this.location = location;
        this.company = company;
    }
}