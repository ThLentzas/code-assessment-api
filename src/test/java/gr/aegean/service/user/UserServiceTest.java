package gr.aegean.service.user;

import gr.aegean.AbstractTestContainers;
import gr.aegean.entity.EmailUpdateToken;
import gr.aegean.entity.User;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.mapper.dto.UserDTOMapper;
import gr.aegean.model.dto.user.UserAccountDeleteRequest;
import gr.aegean.model.dto.user.UserDTO;
import gr.aegean.model.dto.user.UserEmailUpdateRequest;
import gr.aegean.model.dto.user.UserPasswordUpdateRequest;
import gr.aegean.model.dto.user.UserProfile;
import gr.aegean.model.dto.user.UserProfileUpdateRequest;
import gr.aegean.repository.EmailUpdateRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.service.analysis.AnalysisService;
import gr.aegean.service.auth.JwtService;
import gr.aegean.service.email.EmailService;
import gr.aegean.utility.StringUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Random;


@ExtendWith(MockitoExtension.class)
class UserServiceTest extends AbstractTestContainers {
    @Mock
    private EmailService emailService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private JwtService jwtService;
    private UserRepository userRepository;
    private EmailUpdateRepository emailUpdateRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserDTOMapper userDTOMapper = new UserDTOMapper();
    private UserService underTest;

    @BeforeEach
    void setup() {
        userRepository = new UserRepository(getJdbcTemplate());
        emailUpdateRepository = new EmailUpdateRepository(getJdbcTemplate());
        underTest = new UserService(
                jwtService,
                userRepository,
                emailUpdateRepository,
                emailService,
                analysisService,
                passwordEncoder);

        emailUpdateRepository.deleteAllTokens();
        userRepository.deleteAllUsers();
    }

    @Test
    void shouldCreateUser() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        //Act
        underTest.registerUser(user);

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Assert
        assertThat(underTest.findUser()).isNotNull();
    }

    @Test
    void shouldThrowDuplicateResourceExceptionIfEmailAlreadyExists() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User duplicateEmailUser = generateUser();
        duplicateEmailUser.setPassword(duplicateEmailUser.getPassword());
        duplicateEmailUser.setEmail(user.getEmail());

        //Act
        underTest.registerUser(user);

        //Assert
        assertThatThrownBy(() -> underTest.registerUser(duplicateEmailUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already in use");
    }

    @Test
    void shouldThrowDuplicateResourceExceptionIfUsernameAlreadyExists() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        underTest.registerUser(user);

        User duplicateUsernameUser = generateUser();
        duplicateUsernameUser.setPassword(duplicateUsernameUser.getPassword());
        duplicateUsernameUser.setEmail("test2@example.com");
        duplicateUsernameUser.setUsername(user.getUsername());

        //Assert
        assertThatThrownBy(() -> underTest.registerUser(duplicateUsernameUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("The provided username already exists");
    }

    /*
        Password hashing happens after user validation that's why the below password are not hashed.
     */
    @Test
    void shouldThrowIllegalArgumentExceptionWhenFirstnameExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = generateUser();
        user.setFirstname(generateRandomString(random.nextInt(31) + 31));

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid firstname. Firstname must not exceed 30 characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFirstnameContainsNumbers() {
        //Arrange
        User user = generateUser();
        user.setFirstname("T3st");

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid firstname. Firstname should contain only characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFirstnameContainsSpecialCharacters() {
        //Arrange
        User user = generateUser();
        user.setFirstname("T^st");

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid firstname. Firstname should contain only characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenLastnameExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = generateUser();
        user.setLastname(generateRandomString(random.nextInt(31) + 31));

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid lastname. Lastname must not exceed 30 characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenLastnameContainsNumbers() {
        //Arrange
        User user = generateUser();
        user.setLastname("T3st");

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid lastname. Lastname should contain only characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenLastnameContainsSpecialCharacters() {
        //Arrange
        User user = generateUser();
        user.setLastname("T^st");

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid lastname. Lastname should contain only characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUsernameExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = generateUser();
        user.setUsername(generateRandomString(random.nextInt(31) + 31));

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username. Username must not exceed 30 characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailExceedsMaxLength() {
        //Arrange
        Random random = new Random();
        User user = generateUser();
        user.setEmail(generateRandomString(random.nextInt(51) + 51));

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid email. Email must not exceed 50 characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailDoesNotContainAtSymbol() {
        //Arrange
        User user = generateUser();
        user.setEmail("testexample.com");

        //Act Assert
        assertThatThrownBy(() -> underTest.validateUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid email format");
    }

    @Test
    void shouldGetUser() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.registerUser(user);
        UserDTO expected = userDTOMapper.apply(user);

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        UserDTO actual = underTest.findUser();

        //Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFound() {
        //Arrange
        when(jwtService.getSubject()).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.findUser())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + 1);
    }

    @Test
    void shouldUpdateUserProfile() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.registerUser(user);
        UserProfileUpdateRequest profileUpdateRequest = new UserProfileUpdateRequest(
                "foo",
                "foo",
                "Foo",
                "I have a real passion for teaching",
                "Miami, OH",
                "VM, LLC"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        underTest.updateProfile(profileUpdateRequest);

        //Assert
        userRepository.findUserByUserId(user.getId())
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
        UserProfileUpdateRequest profileUpdateRequest = new UserProfileUpdateRequest(
                "foo",
                "foo",
                "Foo",
                "I have a real passion for teaching",
                "Miami, OH",
                "VM, LLC"
        );

        when(jwtService.getSubject()).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.updateProfile(profileUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + 1);
    }

    /*
        Password validation has already being tested.
     */
    @Test
    void shouldUpdateUserPassword() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.registerUser(user);
        UserPasswordUpdateRequest passwordUpdateRequest = new UserPasswordUpdateRequest("Test2Ex@mple", "CyN549^*o2Cr");

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        underTest.updatePassword(passwordUpdateRequest);

        //Assert
        userRepository.findUserByUserId(user.getId())
                .ifPresent(user1 -> assertTrue(passwordEncoder.matches("CyN549^*o2Cr", user1.getPassword())));
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenOldPasswordIsNotCorrect() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        UserPasswordUpdateRequest passwordUpdateRequest = new UserPasswordUpdateRequest("foo", "CyN549^*o2Cr");

        when(jwtService.getSubject()).thenReturn(user.getId().toString());


        //Act Assert
        assertThatThrownBy(() -> underTest.updatePassword(passwordUpdateRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Old password is incorrect");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToUpdatePassword() {
        //Arrange
        UserPasswordUpdateRequest passwordUpdateRequest = new UserPasswordUpdateRequest("foo", "CyN549^*o2Cr");

        when(jwtService.getSubject()).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.updatePassword(passwordUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + 1);
    }

    /*
        Have to override equals() and hashcode() for this to work
     */
    @Test
    void shouldGetUserProfile() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        UserProfile expected = new UserProfile(
                "Test",
                "Test",
                "TestT",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        UserProfile actual = underTest.getProfile();

        //Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToGetProfile() {
        //Arrange
        when(jwtService.getSubject()).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.getProfile())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + 1);
    }

    @Test
    void shouldCreateEmailUpdateToken() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.registerUser(user);

        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "foo@example.com",
                "Test2Ex@mple"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        underTest.createEmailUpdateToken(emailUpdateRequest);

        //Assert
        verify(emailService, times(1)).sendEmailVerification(
                eq(emailUpdateRequest.email()),
                any(String.class),
                any(String.class));
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToUpdateEmail() {
        //Arrange
        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "foo@example.com",
                "test"
        );

        when(jwtService.getSubject()).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.createEmailUpdateToken(emailUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + 1);
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenPasswordIsWrongForEmailUpdateRequest() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);

        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "foo@example.com",
                "foo"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.createEmailUpdateToken(emailUpdateRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Wrong password");
    }

    //Email validation already tested
    @Test
    void shouldThrowDuplicateResourceExceptionWhenNewEmailExistsForEmailUpdateRequest() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.registerUser(user);

        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "test@example.com",
                "Test2Ex@mple"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.createEmailUpdateToken(emailUpdateRequest))
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
        userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("expiredToken");
        LocalDateTime expiryDate = LocalDateTime.now().minusHours(1);
        EmailUpdateToken emailUpdateToken = new EmailUpdateToken(
                user.getId(),
                hashedToken,
                user.getEmail(),
                expiryDate);

        emailUpdateRepository.saveToken(emailUpdateToken);

        //Assert
        assertThatThrownBy(() -> underTest.updateEmail("expiredToken"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("The email verification link has expired. Please request a new one");
    }

    @Test
    void shouldInvalidateAllPreviousTokensWhenNewEmailUpdateTokenIsGenerated() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("token");
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(1);
        EmailUpdateToken emailUpdateToken = new EmailUpdateToken(
                user.getId(),
                hashedToken,
                user.getEmail(),
                expiryDate);

        emailUpdateRepository.saveToken(emailUpdateToken);

        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "foo@example.com",
                "Test2Ex@mple"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        underTest.createEmailUpdateToken(emailUpdateRequest);

        //Assert
        assertThat(emailUpdateRepository.findToken(hashedToken)).isNotPresent();
    }

    @Test
    void shouldUpdateEmail() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("token");
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(1);
        EmailUpdateToken emailUpdateToken = new EmailUpdateToken(
                user.getId(),
                hashedToken,
                user.getEmail(),
                expiryDate);

        emailUpdateRepository.saveToken(emailUpdateToken);

        //Act
        underTest.updateEmail("token");

        //Assert
        assertThat(emailUpdateRepository.findToken(hashedToken)).isNotPresent();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenOnlyFromDateIsProvided(String from) {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.registerUser(user);
        String to = "2020-04-22";

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.getHistory(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Both from and to dates must be provided");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenOnlyToDateIsProvided(String to) {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.registerUser(user);
        String from = "2020-04-22";

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.getHistory(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Both from and to dates must be provided");
    }

    @Test
    void shouldDeleteAccount() {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.registerUser(user);

        UserAccountDeleteRequest accountDeleteRequest = new UserAccountDeleteRequest("Test2Ex@mple");

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        underTest.deleteAccount(accountDeleteRequest);

        //Assert
        assertThatThrownBy(() -> underTest.getProfile())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + user.getId());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenAccountDoesNotExist() {
        //Arrange
        UserAccountDeleteRequest accountDeleteRequest = new UserAccountDeleteRequest("Test2Ex@mple");

        when(jwtService.getSubject()).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.deleteAccount(accountDeleteRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + 1);
    }

    private User generateUser() {
        return User.builder()
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password("Test2Ex@mple")
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();
    }

    private String generateRandomString(int length) {
        return RandomStringUtils.randomAlphabetic(length);
    }
}