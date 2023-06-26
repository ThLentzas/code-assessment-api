package gr.aegean.model.user;

import gr.aegean.model.passwordreset.PasswordResetToken;

import java.util.Objects;

public record UserProfile(
        String firstname,
        String lastname,
        String username,
        String bio,
        String location,
        String company
) {
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(obj == null) {
            return false;
        }

        if(obj instanceof UserProfile profileObj) {
            return firstname.equals(profileObj.firstname)
                    && lastname.equals(profileObj.lastname)
                    && username.equals(profileObj.username)
                    && bio.equals(profileObj.bio)
                    && location.equals(profileObj.location)
                    && company.equals(profileObj.company);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstname, lastname, username, bio, location, company);
    }
}
