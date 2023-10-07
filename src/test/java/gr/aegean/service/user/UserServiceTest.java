package gr.aegean.service.user;

import gr.aegean.AbstractUnitTest;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Random;



@ExtendWith(MockitoExtension.class)
class UserServiceTest extends AbstractUnitTest {
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
    private static final String USER_NOT_FOUND_ERROR_MSG = "User not found with id: ";
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
    void shouldRegisterUser() {
        //Arrange
        User user = generateUser();

        //Act
        underTest.registerUser(user);

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Assert
        assertThat(underTest.findUser()).isNotNull();
    }

    @Test
    void shouldThrowDuplicateResourceExceptionIfEmailAlreadyExistsForRegisterRequest() {
        //Arrange
        User user = generateUser();
        underTest.registerUser(user);

        //email is not case-sensitive so even if the original is "test@example.com" it should still throw an exception
        User duplicateEmailUser = generateUser();
        duplicateEmailUser.setEmail("Test@example.com");

        //Act Assert
        assertThatThrownBy(() -> underTest.registerUser(duplicateEmailUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already in use");
    }

    @Test
    void shouldThrowDuplicateResourceExceptionIfUsernameAlreadyExistsForRegisterRequest() {
        //Arrange
        User user = generateUser();
        underTest.registerUser(user);

        User duplicateUsernameUser = generateUser();
        duplicateUsernameUser.setEmail("test2@example.com");
        duplicateUsernameUser.setUsername(user.getUsername());

        //Act Assert
        assertThatThrownBy(() -> underTest.registerUser(duplicateUsernameUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("The provided username already exists");
    }

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
                .hasMessage(USER_NOT_FOUND_ERROR_MSG + 1);
    }

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
                .hasMessage(USER_NOT_FOUND_ERROR_MSG + 1);
    }

    /*
        The user did not update all of their profile properties, just some of them.
     */
    @Test
    void shouldUpdateUserProfile() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        UserProfileUpdateRequest profileUpdateRequest = new UserProfileUpdateRequest(
                "foo",
                null,
                null,
                "I have a real passion for teaching",
                "Miami, OH",
                "VM, LLC"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        underTest.updateProfile(profileUpdateRequest);

        //Assert
        userRepository.findUserById(user.getId())
                .ifPresent(actual -> {
                    assertThat(actual.getFirstname()).isEqualTo(profileUpdateRequest.firstname());
                    assertThat(actual.getBio()).isEqualTo(profileUpdateRequest.bio());
                    assertThat(actual.getLocation()).isEqualTo(profileUpdateRequest.location());
                    assertThat(actual.getCompany()).isEqualTo(profileUpdateRequest.company());
                });
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenUsernameAlreadyExistsForProfileUpdateRequest() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        UserProfileUpdateRequest profileUpdateRequest = new UserProfileUpdateRequest(
                "foo",
                "foo",
                "TestT",
                "I have a real passion for teaching",
                "Miami, OH",
                "VM, LLC"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.updateProfile(profileUpdateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("The provided username already exists");
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
                .hasMessage(USER_NOT_FOUND_ERROR_MSG + 1);
    }

    /*
        Password validation has already being tested, we don't have to test that the new password meets all the
        requirements, will test that the exception is thrown and handled correctly in the Controller. Same case with
        validating constraints and preferences for both analysis request and refresh request. We only did for the
        analysis request.
     */
    @Test
    void shouldUpdateUserPassword() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        UserPasswordUpdateRequest passwordUpdateRequest = new UserPasswordUpdateRequest("Igw4UQAlfX$E", "3frMH4v!20d4");

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        underTest.updatePassword(passwordUpdateRequest);

        //Assert
        userRepository.findUserById(user.getId())
                .ifPresent(actual -> assertTrue(passwordEncoder.matches("3frMH4v!20d4", actual.getPassword())));
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenOldPasswordIsWrong() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        UserPasswordUpdateRequest passwordUpdateRequest = new UserPasswordUpdateRequest("foo", "3frMH4v!20d4");

        when(jwtService.getSubject()).thenReturn(user.getId().toString());


        //Act Assert
        assertThatThrownBy(() -> underTest.updatePassword(passwordUpdateRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Old password is wrong");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToUpdatePassword() {
        //Arrange
        UserPasswordUpdateRequest passwordUpdateRequest = new UserPasswordUpdateRequest("foo", "CyN549^*o2Cr");

        when(jwtService.getSubject()).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.updatePassword(passwordUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(USER_NOT_FOUND_ERROR_MSG + 1);
    }

    @Test
    void shouldCreateEmailUpdateToken() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "foo@example.com",
                "Igw4UQAlfX$E"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());
        doNothing().when(emailService).sendEmailVerification(
                eq(emailUpdateRequest.email()),
                any(String.class),
                any(String.class));

        //Act
        underTest.createEmailUpdateToken(emailUpdateRequest);

        //Assert
        verify(emailService, times(1)).sendEmailVerification(
                eq(emailUpdateRequest.email()),
                any(String.class),
                any(String.class));
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
        userRepository.registerUser(user);
        UserEmailUpdateRequest emailUpdateRequest = new UserEmailUpdateRequest(
                "test@example.com",
                "Igw4UQAlfX$E"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.createEmailUpdateToken(emailUpdateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already in use");
    }

    /*
        An empty string is enough. There is no need to test more cases like {"\t", "\n", "  "} because .isBlank()
        will handle them. Token will never be null because there is a default value of an empty string that is given
        in the controller
     */
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"malformedToken"})
    void shouldNotUpdateEmailWhenEmailUpdateTokenIsBlankOrMalformed(String token) {
        //Act
        boolean actual = underTest.updateEmail(token);

        //Assert
        assertThat(actual).isFalse();
    }

    @Test
    void shouldNotUpdateEmailWhenEmailUpdateTokenExpired() {
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

        //Act
        boolean actual = underTest.updateEmail("expiredToken");

        //Assert
        assertThat(actual).isFalse();
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
                .hasMessage(USER_NOT_FOUND_ERROR_MSG + 1);
    }

    @Test
    void shouldInvalidateAllPreviousTokensWhenNewEmailUpdateTokenIsGenerated() {
        //Arrange
        User user = generateUser();
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
                "Igw4UQAlfX$E"
        );

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        underTest.createEmailUpdateToken(emailUpdateRequest);

        //Assert
        assertThat(emailUpdateRepository.findToken(hashedToken)).isNotPresent();
    }

    /*
        We don't mock the jwtService, because this is the permitAll() endpoint, where user clicks the link on their
        email
     */
    @Test
    void shouldUpdateEmail() {
        //Arrange
        User user = generateUser();
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
        boolean actual = underTest.updateEmail("token");

        //Assert
        assertThat(actual).isTrue();
        assertThat(emailUpdateRepository.findToken(hashedToken)).isNotPresent();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowIllegalArgumentExceptionWhenFromDateIsNullOrEmpty(String from) {
        //Arrange
        User user = generateUser();
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
    void shouldThrowIllegalArgumentExceptionWhenToDateIsNullOrEmpty(String to) {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        String from = "2020-04-22";

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.getHistory(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Both from and to dates must be provided");
    }

    /*
        Supported format is yyyy-MM-dd
     */
    @Test
    void shouldThrowIllegalArgumentExceptionWhenAtLeastOneDateIsNotInTheSupportedFormat() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        String from = "22-04-2020";
        String to = "2020-04-22";

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.getHistory(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The provided date is invalid: " + from);
    }

    @Test
    void shouldDeleteAccount() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        UserAccountDeleteRequest accountDeleteRequest = new UserAccountDeleteRequest("Igw4UQAlfX$E");

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        underTest.deleteAccount(accountDeleteRequest);

        //Assert
        assertThatThrownBy(() -> underTest.getProfile())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(USER_NOT_FOUND_ERROR_MSG + user.getId());
    }

    @Test
    void shouldThrowBadCredentialExceptionWhenPasswordIsWrongForAccountDeletion() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);
        UserAccountDeleteRequest accountDeleteRequest = new UserAccountDeleteRequest("foo");

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act Assert
        assertThatThrownBy(() -> underTest.deleteAccount(accountDeleteRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Password is wrong");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenAccountDoesNotExist() {
        //Arrange
        UserAccountDeleteRequest accountDeleteRequest = new UserAccountDeleteRequest("Test2Ex@mple");

        when(jwtService.getSubject()).thenReturn(String.valueOf(1));

        //Act Assert
        assertThatThrownBy(() -> underTest.deleteAccount(accountDeleteRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(USER_NOT_FOUND_ERROR_MSG + 1);
    }

    /*
        Password hashing happens after user validation in the authentication service, so when userService.registerUser()
        is called the password is already hashed. For db consistency we hash the password before registering the user
     */
    private User generateUser() {
        return User.builder()
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("Igw4UQAlfX$E"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();
    }

    private String generateRandomString(int length) {
        return RandomStringUtils.randomAlphabetic(length);
    }
}