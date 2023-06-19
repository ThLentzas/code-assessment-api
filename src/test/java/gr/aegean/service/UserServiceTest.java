package gr.aegean.service;

import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.model.user.User;
import gr.aegean.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest extends AbstractTestContainers{
    private UserRepository userRepository;
    private UserService underTest;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setup() {
        userRepository = new UserRepository(getJdbcTemplate());
        underTest = new UserService(userRepository);

        userRepository.deleteAllUsers();
    }

    @Test
    void shouldCreateUserAndReturnTheGeneratedID() {
        //Arrange
        User user = generateUser();

        //Act
        Integer userID = underTest.registerUser(user);

        //Assert
        assertThat(userID).isNotNull();
    }

    @Test
    void shouldThrowDuplicateResourceExceptionIfEmailAlreadyExists() {
        //Arrange
        User user1 = generateUser();
        User user2 = generateUser();

        //Act
        underTest.registerUser(user1);

        //Assert
        assertThatThrownBy(() -> underTest.registerUser(user2))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("The provided email already exists");
    }

    @Test
    void shouldThrowDuplicateResourceExceptionIfUsernameAlreadyExists() {
        //Arrange
        User user1 = generateUser();
        User user2 = generateUser();

        //Act
        underTest.registerUser(user1);
        user2.setEmail("test2@example.com");

        //Assert
        assertThatThrownBy(() -> underTest.registerUser(user2))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("The provided username already exists");
    }


    private User generateUser() {
        return new User(
                "Test",
                "Test",
                "TestT",
                "test@example.com",
                passwordEncoder.encode("test"),
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );
    }
}
