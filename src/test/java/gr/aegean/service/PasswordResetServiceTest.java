package gr.aegean.service;

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

import gr.aegean.model.user.User;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.security.password.PasswordResetRequest;
import gr.aegean.security.password.PasswordResetResult;
import gr.aegean.exception.BadCredentialsException;
import gr.aegean.security.password.PasswordResetConfirmationRequest;
import gr.aegean.security.password.PasswordResetToken;
import gr.aegean.utility.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest extends AbstractTestContainers{
    @Mock
    private EmailService emailService;
    private UserRepository userRepository;
    private PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private PasswordResetService underTest;

    @BeforeEach
    void setup() {
        passwordResetRepository = new PasswordResetRepository(getJdbcTemplate());
        userRepository = new UserRepository(getJdbcTemplate());
        underTest = new PasswordResetService(
                emailService,
                userRepository,
                passwordResetRepository,
                passwordEncoder);

        passwordResetRepository.deleteAllTokens();
        userRepository.deleteAllUsers();
    }

    @Test
    void shouldCreatePasswordResetTokenWhenUserIsFound() {
        PasswordResetRequest passwordResetRequest = new PasswordResetRequest("test@example.com");
        User user = generateUser();
        userRepository.registerUser(user);

        PasswordResetResult passwordResetResult = underTest.createPasswordResetToken(passwordResetRequest);

        assertThat(passwordResetResult.message()).isEqualTo(
                "If your email address exists in our database, you will receive a password recovery link at " +
                        "your email address in a few minutes.");

        verify(emailService, times(1)).sendPasswordResetLinkEmail(
                eq(user.getEmail()),
                any(String.class));
    }

    @Test
    void shouldNotCreatePasswordResetTokenWhenUserIsNotFound() {
        PasswordResetRequest passwordResetRequest = new PasswordResetRequest("test1@example.com");
        User user = generateUser();
        userRepository.registerUser(user);

        PasswordResetResult passwordResetResult = underTest.createPasswordResetToken(passwordResetRequest);

        assertThat(passwordResetResult.message()).isEqualTo(
                "If your email address exists in our database, you will receive a password recovery link at " +
                        "your email address in a few minutes.");

        verify(emailService, never()).sendPasswordResetLinkEmail(eq(user.getEmail()), any(String.class));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void shouldThrowBadCredentialsExceptionWhenEmailIsInvalid(String invalidEmail) {
        PasswordResetRequest passwordResetRequest = new PasswordResetRequest(invalidEmail);
        assertThatThrownBy(() -> underTest.createPasswordResetToken(passwordResetRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("The Email field is required.");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {"invalidToken"})
    void shouldThrowBadCredentialsExceptionWhenTokenIsInvalid(String invalidToken) {
        //Arrange Act Assert
        assertThatThrownBy(() -> underTest.validatePasswordResetToken(invalidToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Reset password token is invalid");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenTokenExpired() {
        //Arrange
        User user = generateUser();
        Integer userId= userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("expiredToken");
        PasswordResetToken passwordResetToken = new PasswordResetToken(
                userId,
                hashedToken,
                LocalDateTime.now().minusHours(1));
        //Act
        passwordResetRepository.createPasswordResetToken(passwordResetToken);

        //Assert
        assertThatThrownBy(() -> underTest.validatePasswordResetToken("expiredToken"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("The password reset link has expired. Please request a new one.");
    }

    /*
        No need to test for the password encoder or to validate the updated password or the email service because they
        have been tested separately in email service, etc.
     */
    @Test
    void shouldResetPassword() {
        //Arrange
        User user = generateUser();
        Integer userId= userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("token");
        PasswordResetToken passwordResetToken = new PasswordResetToken(
                userId,
                hashedToken,
                LocalDateTime.now().plusHours(2));
        passwordResetRepository.createPasswordResetToken(passwordResetToken);

        PasswordResetConfirmationRequest passwordResetConfirmationRequest = new PasswordResetConfirmationRequest(
                "token",
                "3frMH4v!20d4");

        //Act
        underTest.resetPassword(passwordResetConfirmationRequest);

        //Assert
        verify(emailService, times(1)).sendPasswordResetConfirmationEmail(
                user.getEmail(),
                user.getUsername());
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
