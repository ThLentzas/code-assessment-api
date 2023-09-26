package gr.aegean.entity;

import lombok.*;


@Getter
@Setter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
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

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }
}