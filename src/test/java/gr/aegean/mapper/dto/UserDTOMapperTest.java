package gr.aegean.mapper.dto;

import gr.aegean.entity.User;
import gr.aegean.model.user.UserDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class UserDTOMapperTest {
    private UserDTOMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new UserDTOMapper();
    }

    @Test
    void shouldConvertUserToUserDTO() {
        //Arrange
        User expected = User.builder()
                .id(1)
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act
        UserDTO actual = underTest.apply(expected);

        //Assert
        assertThat(expected.getId()).isEqualTo(actual.id());
        assertThat(expected.getFirstname()).isEqualTo(actual.firstname());
        assertThat(expected.getLastname()).isEqualTo(actual.lastname());
        assertThat(expected.getUsername()).isEqualTo(actual.username());
        assertThat(expected.getEmail()).isEqualTo(actual.email());
        assertThat(expected.getBio()).isEqualTo(actual.bio());
        assertThat(expected.getLocation()).isEqualTo(actual.location());
        assertThat(expected.getCompany()).isEqualTo(actual.company());
    }
}
