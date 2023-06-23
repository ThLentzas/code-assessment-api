package gr.aegean.service;

import gr.aegean.exception.BadCredentialsException;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.model.user.User;
import gr.aegean.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

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

    @Test
    void shouldThrowBadCredentialsExceptionWhenFirstnameExceedsMaxLength() {
        Random random = new Random();
        User user = new User(
                generateRandomString(random.nextInt(31) + 31),
                "Test",
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid firstname. Too many characters");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenFirstnameContainsNumbers() {
        //Arrange
        User user = new User(
                "T3st",
                "Test",
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid firstname. Name should contain only characters");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenFirstnameContainsSpecialCharacters() {
        //Arrange
        User user = new User(
                "T^st",
                "Test",
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid firstname. Name should contain only characters");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenLastnameExceedsMaxLength() {
        Random random = new Random();
        User user = new User(
                "Test",
                generateRandomString(random.nextInt(31) + 31),
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid lastname. Too many characters");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenLastnameContainsNumbers() {
        //Arrange
        User user = new User(
                "Test",
                "T3st",
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid lastname. Name should contain only characters");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenLastnameContainsSpecialCharacters() {
        //Arrange
        User user = new User(
                "Test",
                "T^st",
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid lastname. Name should contain only characters");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenUsernameExceedsMaxLength() {
        Random random = new Random();
        User user = new User(
                "Test",
                "TestT",
                generateRandomString(random.nextInt(31) + 31),
                "test@gmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username. Too many characters");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenEmailExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = new User(
                "Test",
                "Test",
                "TestT",
                generateRandomString(random.nextInt(51) + 51),
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email. Too many characters");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenEmailDoesNotContainAtSymbol() {
        //Arrange
        User user = new User(
                "Test",
                "Test",
                "TestT",
                "testgmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenBioExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = new User(
                "Test",
                "Test",
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr",
                generateRandomString(random.nextInt(151) + 151),
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid bio. Too many characters");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenLocationExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = new User(
                "Test",
                "Test",
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                generateRandomString(random.nextInt(51) + 51),
                "Code Monkey, LLC"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid location. Too many characters");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenRegisterCompanyExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = new User(
                "Test",
                "Test",
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                generateRandomString(random.nextInt(51) + 51)
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid company. Too many characters");
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

    private String generateRandomString(int length) {
        return RandomStringUtils.randomAlphabetic(length);
    }
}
