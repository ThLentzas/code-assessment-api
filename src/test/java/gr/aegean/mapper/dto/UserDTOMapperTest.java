package gr.aegean.mapper.dto;

import gr.aegean.entity.User;
import gr.aegean.model.user.UserDTO;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class UserDTOMapperTest {
    private UserDTOMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new UserDTOMapper();
    }

    @Test
    void shouldConvertUserToUserDTO() {

        User user = User.builder()
                .id(1)
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        UserDTO userDTO = underTest.apply(user);

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
