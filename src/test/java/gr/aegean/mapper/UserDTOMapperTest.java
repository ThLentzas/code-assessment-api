package gr.aegean.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import gr.aegean.model.user.User;
import gr.aegean.model.user.UserDTO;
import org.junit.jupiter.api.Test;

class UserDTOMapperTest {

    @Test
    void shouldConvertUserToUserDTO() {
        UserDTOMapper mapper = new UserDTOMapper();

        User user = new User(
                1,
                "Test",
                "Test",
                "TestT",
                "test@example.com",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        UserDTO userDTO = mapper.apply(user);

        assertThat(user.getId()).isEqualTo(userDTO.id());
        assertThat(user.getFirstname()).isEqualTo(userDTO.firstname());
        assertThat(user.getLastname()).isEqualTo(userDTO.lastname());
        assertThat(user.getUsername()).isEqualTo(userDTO.username());
        assertThat(user.getEmail()).isEqualTo(userDTO.email());
        assertThat(user.getBio()).isEqualTo(userDTO.bio());
        assertThat(user.getLocation()).isEqualTo(userDTO.location());
        assertThat(user.getCompany()).isEqualTo(userDTO.company());
    }
}
