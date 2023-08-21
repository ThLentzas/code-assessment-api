package gr.aegean.mapper.dto;

import gr.aegean.entity.User;
import gr.aegean.model.user.UserDTO;

import java.util.function.Function;


public class UserDTOMapper implements Function<User, UserDTO> {

    @Override
    public UserDTO apply(User user) {
        return new UserDTO(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getUsername(),
                user.getEmail(),
                user.getBio(),
                user.getLocation(),
                user.getCompany()
        );
    }
}
