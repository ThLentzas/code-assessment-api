package gr.aegean.service;

import gr.aegean.AbstractTestContainers;
import gr.aegean.exception.BadCredentialsException;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.model.token.EmailUpdateToken;
import gr.aegean.model.user.User;
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
    private UserService underTest;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setup() {
        userRepository = new UserRepository(getJdbcTemplate());
        emailUpdateRepository = new EmailUpdateRepository(getJdbcTemplate());
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
    void shouldThrowBadCredentialsExceptionWhenCompanyExceedsMaxLength() {
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
