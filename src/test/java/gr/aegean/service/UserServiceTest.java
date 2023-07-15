package gr.aegean.service;

import gr.aegean.AbstractTestContainers;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.mapper.EmailUpdateTokenRowMapper;
import gr.aegean.mapper.UserRowMapper;
import gr.aegean.model.entity.EmailUpdateToken;
import gr.aegean.model.entity.User;
import gr.aegean.model.user.UserEmailUpdateRequest;
import gr.aegean.model.user.UserPasswordUpdateRequest;
import gr.aegean.model.user.UserProfile;
import gr.aegean.model.user.UserProfileUpdateRequest;
import gr.aegean.repository.EmailUpdateRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.utility.StringUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Random;


@ExtendWith(MockitoExtension.class)
class UserServiceTest extends AbstractTestContainers {
    @Mock
    private EmailService emailService;
    private UserRepository userRepository;
    private EmailUpdateRepository emailUpdateRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserRowMapper userMapper = new UserRowMapper();
    private final EmailUpdateTokenRowMapper tokenRowMapper = new EmailUpdateTokenRowMapper();
    private UserService underTest;

    @BeforeEach
    void setup() {
        userRepository = new UserRepository(getJdbcTemplate(), userMapper);
        emailUpdateRepository = new EmailUpdateRepository(getJdbcTemplate(), tokenRowMapper);
        underTest = new UserService(userRepository, emailUpdateRepository, emailService, passwordEncoder);

        emailUpdateRepository.deleteAllTokens();
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
                .hasMessage("Email already in use");
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
    void shouldThrowIllegalArgumentExceptionWhenFirstnameExceedsMaxLength() {
        Random random = new Random();
        User user = User.builder()
                .firstname(generateRandomString(random.nextInt(31) + 31))
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid firstname. Too many characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFirstnameContainsNumbers() {
        //Arrange
        User user = User.builder()
                .firstname("T3st")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid firstname. Name should contain only characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFirstnameContainsSpecialCharacters() {
        //Arrange
        User user = User.builder()
                .firstname("T^st")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid firstname. Name should contain only characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenLastnameExceedsMaxLength() {
        Random random = new Random();
        User user = User.builder()
                .firstname("Test")
                .lastname(generateRandomString(random.nextInt(31) + 31))
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid lastname. Too many characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenLastnameContainsNumbers() {
        //Arrange
        User user = User.builder()
                .firstname("Test")
                .lastname("T3st")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid lastname. Name should contain only characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenLastnameContainsSpecialCharacters() {
        //Arrange
        User user = User.builder()
                .firstname("Test")
                .lastname("T^st")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid lastname. Name should contain only characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUsernameExceedsMaxLength() {
        Random random = new Random();
        User user = User.builder()
                .firstname("Test")
                .lastname("Test")
                .username(generateRandomString(random.nextInt(31) + 31))
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username. Too many characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = User.builder()
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email(generateRandomString(random.nextInt(51) + 51))
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid email. Too many characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailDoesNotContainAtSymbol() {
        //Arrange
        User user = User.builder()
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("testexample.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid email");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenBioExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = User.builder()
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio(generateRandomString(random.nextInt(151) + 151))
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid bio. Too many characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenLocationExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = User.builder()
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location(generateRandomString(random.nextInt(51) + 51))
                .company("Code Monkey, LLC")
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid location. Too many characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCompanyExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = User.builder()
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company(generateRandomString(random.nextInt(51) + 51))
                .build();

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid company. Too many characters");
    }

    @Test
    void shouldUpdateUserProfile() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);
        UserProfileUpdateRequest profileUpdateRequest = new UserProfileUpdateRequest(
                "foo",
                "foo",
                "Foo",
                "I have a real passion for teaching",
                "Miami, OH",
                "VM, LLC"
        );

        //Act
        underTest.updateProfile(userId, profileUpdateRequest);

        //Assert
        userRepository.findUserByUserId(userId)
                .ifPresent(user1 -> {
                    assertThat(user1.getFirstname()).isEqualTo(profileUpdateRequest.firstname());
                    assertThat(user1.getLastname()).isEqualTo(profileUpdateRequest.lastname());
                    assertThat(user1.getUsername()).isEqualTo(profileUpdateRequest.username());
                    assertThat(user1.getBio()).isEqualTo(profileUpdateRequest.bio());
                    assertThat(user1.getLocation()).isEqualTo(profileUpdateRequest.location());
                    assertThat(user1.getCompany()).isEqualTo(profileUpdateRequest.company());
                });
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToUpdateProfile() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);
        UserProfileUpdateRequest profileUpdateRequest = new UserProfileUpdateRequest(
                "foo",
                "foo",
                "Foo",
                "I have a real passion for teaching",
                "Miami, OH",
                "VM, LLC"
        );
        Integer nonExistingId = userId + 1;

        //Act Assert
        assertThatThrownBy(() -> underTest.updateProfile(nonExistingId, profileUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id: " + nonExistingId + " not found");
    }

    /*
        Password validation has already being tested.
     */
    @Test
    void shouldUpdateUserPassword() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);
        UserPasswordUpdateRequest passwordUpdateRequest = new UserPasswordUpdateRequest("test", "CyN549^*o2Cr");

        //Act
        underTest.updatePassword(userId, passwordUpdateRequest);

        //Assert
        userRepository.findUserByUserId(userId)
                .ifPresent(user1 -> assertTrue(passwordEncoder.matches("CyN549^*o2Cr", user1.getPassword())));
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenOldPasswordIsNotCorrect() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);
        UserPasswordUpdateRequest passwordUpdateRequest = new UserPasswordUpdateRequest("foo", "CyN549^*o2Cr");

        //Act Assert
        assertThatThrownBy(() -> underTest.updatePassword(userId, passwordUpdateRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Old password is incorrect");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToUpdatePassword() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);
        UserPasswordUpdateRequest passwordUpdateRequest = new UserPasswordUpdateRequest("foo", "CyN549^*o2Cr");
        Integer nonExistingId = userId + 1;

        //Act Assert
        assertThatThrownBy(() -> underTest.updatePassword(nonExistingId, passwordUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id: " + nonExistingId + " not found");
    }

    /*
        Have to override equals() and hashcode() for this to work
     */
    @Test
    void shouldGetUserProfile() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);
        UserProfile expected = new UserProfile(
                "Test",
                "Test",
                "TestT",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        //Act
        UserProfile actual = underTest.getProfile(userId);

        //Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToGetProfile() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);
        Integer nonExistingId = userId + 1;

        //Act Assert
        assertThatThrownBy(() -> underTest.getProfile(nonExistingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id: " + nonExistingId + " not found");
    }

    @Test
    void shouldCreateEmailUpdateToken() {
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "foo@example.com",
                "test"
        );

        underTest.createEmailUpdateToken(userId, emailUpdateRequest);

        verify(emailService, times(1)).sendEmailVerification(
                eq(emailUpdateRequest.email()),
                any(String.class),
                any(String.class));
    }


    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToUpdateEmail() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);
        Integer nonExistingId = userId + 1;

        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "foo@example.com",
                "test"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.createEmailUpdateToken(nonExistingId, emailUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id: " + nonExistingId + " not found");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenPasswordIsWrongForEmailUpdateRequest() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "foo@example.com",
                "foo"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.createEmailUpdateToken(userId, emailUpdateRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Wrong password");
    }

    //Email validation already tested
    @Test
    void shouldThrowDuplicateResourceExceptionWhenNewEmailExistsForEmailUpdateRequest() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "test@example.com",
                "test"
        );

        //Act Assert
        assertThatThrownBy(() -> underTest.createEmailUpdateToken(userId, emailUpdateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already in use");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {"invalidToken"})
    void shouldThrowBadCredentialsExceptionWhenEmailUpdateTokenIsInvalid(String invalidToken) {
        //Arrange Act Assert
        assertThatThrownBy(() -> underTest.updateEmail(invalidToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email update token is invalid");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenEmailUpdateTokenExpired() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("expiredToken");
        LocalDateTime expiryDate = LocalDateTime.now().minusHours(1);
        EmailUpdateToken emailUpdateToken = new EmailUpdateToken(
                userId,
                hashedToken,
                user.getEmail(),
                expiryDate);

        emailUpdateRepository.createToken(emailUpdateToken);

        //Assert
        assertThatThrownBy(() -> underTest.updateEmail("expiredToken"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("The email verification link has expired. Please request a new one.");
    }

    @Test
    void shouldUpdateEmail() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("token");
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(1);
        EmailUpdateToken emailUpdateToken = new EmailUpdateToken(
                userId,
                hashedToken,
                user.getEmail(),
                expiryDate);

        emailUpdateRepository.createToken(emailUpdateToken);

        //Act
        underTest.updateEmail("token");

        //Assert
        assertThat(emailUpdateRepository.findToken(hashedToken)).isNotPresent();
    }

    private User generateUser() {
        return User.builder()
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();
    }


    private String generateRandomString(int length) {
        return RandomStringUtils.randomAlphabetic(length);
    }
}
