package gr.aegean.mapper;

import gr.aegean.model.user.User;
import gr.aegean.model.user.UserDTO;

import org.springframework.stereotype.Service;

import java.util.function.Function;


@Service
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
