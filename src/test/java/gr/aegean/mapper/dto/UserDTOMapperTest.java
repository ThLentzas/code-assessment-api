package gr.aegean.mapper.dto;

import gr.aegean.entity.User;
import gr.aegean.model.dto.user.UserDTO;

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
    void shouldMapUserToUserDTO() {
        //Arrange
        User expected = User.builder()
                .id(1)
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password("Test")
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act
        UserDTO actual = underTest.apply(expected);

        //Assert
        assertThat(actual.id()).isEqualTo(expected.getId());
        assertThat(actual.firstname()).isEqualTo(expected.getFirstname());
        assertThat(actual.lastname()).isEqualTo(expected.getLastname());
        assertThat(actual.username()).isEqualTo(expected.getUsername());
        assertThat(actual.email()).isEqualTo(expected.getEmail());
        assertThat(actual.bio()).isEqualTo(expected.getBio());
        assertThat(actual.location()).isEqualTo(expected.getLocation());
        assertThat(actual.company()).isEqualTo(expected.getCompany());
    }
}
