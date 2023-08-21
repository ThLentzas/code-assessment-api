package gr.aegean.service.user;

import gr.aegean.AbstractTestContainers;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.entity.EmailUpdateToken;
import gr.aegean.entity.User;
import gr.aegean.model.user.UserUpdateEmailRequest;
import gr.aegean.model.user.UserUpdatePasswordRequest;
import gr.aegean.model.user.UserProfile;
import gr.aegean.model.user.UserProfileUpdateRequest;
import gr.aegean.repository.EmailUpdateRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.service.analysis.AnalysisService;
import gr.aegean.service.auth.AuthService;
import gr.aegean.service.auth.CookieService;
import gr.aegean.service.auth.JwtService;
import gr.aegean.service.email.EmailService;
import gr.aegean.utility.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
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
import static org.mockito.Mockito.mock;
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
    private CookieService cookieService;
    @Mock
    private JwtService jwtService;
    private UserRepository userRepository;
    private EmailUpdateRepository emailUpdateRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService underTest;

    @BeforeEach
    void setup() {
        userRepository = new UserRepository(getJdbcTemplate());
        emailUpdateRepository = new EmailUpdateRepository(getJdbcTemplate());
        underTest = new UserService(
                cookieService,
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
        //Arrange
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
        //Arrange
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
        //Arrange
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
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(userId.toString());

        //Act
        underTest.updateProfile(mockRequest, profileUpdateRequest);

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
        UserProfileUpdateRequest profileUpdateRequest = new UserProfileUpdateRequest(
                "foo",
                "foo",
                "Foo",
                "I have a real passion for teaching",
                "Miami, OH",
                "VM, LLC"
        );
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.updateProfile(mockRequest, profileUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id: " + 1 + " not found");
    }

    /*
        Password validation has already being tested.
     */
    @Test
    void shouldUpdateUserPassword() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);
        UserUpdatePasswordRequest passwordUpdateRequest = new UserUpdatePasswordRequest("test", "CyN549^*o2Cr");
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(userId.toString());

        //Act
        underTest.updatePassword(mockRequest, passwordUpdateRequest);

        //Assert
        userRepository.findUserByUserId(userId)
                .ifPresent(user1 -> assertTrue(passwordEncoder.matches("CyN549^*o2Cr", user1.getPassword())));
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenOldPasswordIsNotCorrect() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);
        UserUpdatePasswordRequest passwordUpdateRequest = new UserUpdatePasswordRequest("foo", "CyN549^*o2Cr");
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(userId.toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.updatePassword(mockRequest, passwordUpdateRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Old password is incorrect");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToUpdatePassword() {
        //Arrange
        UserUpdatePasswordRequest passwordUpdateRequest = new UserUpdatePasswordRequest("foo", "CyN549^*o2Cr");
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.updatePassword(mockRequest, passwordUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id: " + 1 + " not found");
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
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(userId.toString());

        //Act
        UserProfile actual = underTest.getProfile(mockRequest);

        //Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToGetProfile() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(String.valueOf(1));

        //Arrange Act Assert
        assertThatThrownBy(() -> underTest.getProfile(mockRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id: " + 1 + " not found");
    }

    @Test
    void shouldCreateEmailUpdateToken() {
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        UserUpdateEmailRequest emailUpdateRequest = new UserUpdateEmailRequest(
                "foo@example.com",
                "test"
        );
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(userId.toString());

        underTest.createEmailUpdateToken(mockRequest, emailUpdateRequest);

        verify(emailService, times(1)).sendEmailVerification(
                eq(emailUpdateRequest.email()),
                any(String.class),
                any(String.class));
    }


    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToUpdateEmail() {
        //Arrange
        UserUpdateEmailRequest emailUpdateRequest = new UserUpdateEmailRequest(
                "foo@example.com",
                "test"
        );

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.createEmailUpdateToken(mockRequest, emailUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id: " + 1 + " not found");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenPasswordIsWrongForEmailUpdateRequest() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        UserUpdateEmailRequest emailUpdateRequest = new UserUpdateEmailRequest(
                "foo@example.com",
                "foo"
        );
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(userId.toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.createEmailUpdateToken(mockRequest, emailUpdateRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Wrong password");
    }

    //Email validation already tested
    @Test
    void shouldThrowDuplicateResourceExceptionWhenNewEmailExistsForEmailUpdateRequest() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        UserUpdateEmailRequest emailUpdateRequest = new UserUpdateEmailRequest(
                "test@example.com",
                "test"
        );
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(userId.toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.createEmailUpdateToken(mockRequest, emailUpdateRequest))
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

        emailUpdateRepository.saveToken(emailUpdateToken);

        //Assert
        assertThatThrownBy(() -> underTest.updateEmail("expiredToken"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("The email verification link has expired. Please request a new one.");
    }

    @Test
    void shouldInvalidateAllPreviousTokensWhenNewEmailUpdateTokenIsGenerated() {
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

        emailUpdateRepository.saveToken(emailUpdateToken);

        UserUpdateEmailRequest emailUpdateRequest = new UserUpdateEmailRequest(
                "foo@example.com",
                "test"
        );
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(userId.toString());

        //Act
        underTest.createEmailUpdateToken(mockRequest, emailUpdateRequest);

        //Assert
        assertThat(emailUpdateRepository.findToken(hashedToken)).isNotPresent();
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

        emailUpdateRepository.saveToken(emailUpdateToken);

        //Act
        underTest.updateEmail("token");

        //Assert
        assertThat(emailUpdateRepository.findToken(hashedToken)).isNotPresent();
    }


    @Test
    void shouldDeleteAccount() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(userId.toString());

        //Act
        underTest.deleteAccount(mockRequest);

        //Assert
        assertThatThrownBy(() -> underTest.getProfile(mockRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id: " + userId + " not found");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenAccountDoesNotExist() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.getSubject(any(String.class))).thenReturn(String.valueOf(1));

        //Arrange Act Assert
        assertThatThrownBy(() -> underTest.deleteAccount(mockRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No account was found with the provided: " + 1);
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