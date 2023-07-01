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
import gr.aegean.model.passwordreset.PasswordResetRequest;
import gr.aegean.model.passwordreset.PasswordResetResult;
import gr.aegean.model.token.PasswordResetToken;
import gr.aegean.model.passwordreset.PasswordResetConfirmationRequest;
import gr.aegean.exception.BadCredentialsException;
import gr.aegean.repository.UserRepository;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.utility.StringUtils;
import gr.aegean.AbstractTestContainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest extends AbstractTestContainers {
    @Mock
    private EmailService emailService;
    private PasswordResetRepository passwordResetRepository;
    private UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private PasswordResetService underTest;

    @BeforeEach
    void setup() {
        userRepository = new UserRepository(getJdbcTemplate());
        passwordResetRepository = new PasswordResetRepository(getJdbcTemplate());
        underTest = new PasswordResetService(
                emailService,
                passwordResetRepository,
                userRepository,
                passwordEncoder);

        passwordResetRepository.deleteAllTokens();
        userRepository.deleteAllUsers();
    }

    @Test
    void shouldCreatePasswordResetTokenWhenUserIsFound() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);

        PasswordResetRequest passwordResetRequest = new PasswordResetRequest("test@example.com");

        //Act
        PasswordResetResult passwordResetResult = underTest.createPasswordResetToken(passwordResetRequest);

        //Assert
        assertThat(passwordResetResult.message()).isEqualTo(
                "If your email address exists in our database, you will receive a password recovery link at " +
                        "your email address in a few minutes.");

        verify(emailService, times(1)).sendPasswordResetRequestEmail(
                eq(user.getEmail()),
                any(String.class));
    }

    @Test
    void shouldNotCreatePasswordResetTokenWhenUserIsNotFound() {
        PasswordResetRequest passwordResetRequest = new PasswordResetRequest("test1@example.com");

        //Act
        PasswordResetResult passwordResetResult = underTest.createPasswordResetToken(passwordResetRequest);

        //Assert
        assertThat(passwordResetResult.message()).isEqualTo(
                "If your email address exists in our database, you will receive a password recovery link at " +
                        "your email address in a few minutes.");

        verifyNoInteractions(emailService);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {"invalidToken"})
    void shouldThrowBadCredentialsExceptionWhenPasswordResetTokenIsInvalid(String invalidToken) {
        //Arrange Act Assert
        assertThatThrownBy(() -> underTest.validatePasswordResetToken(invalidToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Reset password token is invalid");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenPasswordResetTokenExpired() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("expiredToken");
        LocalDateTime expiryDate = LocalDateTime.now().minusHours(1);
        PasswordResetToken passwordResetToken = new PasswordResetToken(
                userId,
                hashedToken,
                expiryDate);

        passwordResetRepository.createToken(passwordResetToken);

        //Assert
        assertThatThrownBy(() -> underTest.validatePasswordResetToken("expiredToken"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("The password reset link has expired. Please request a new one.");
    }

    /*
        No need to test for the password encoder or to validate the updated password or the email service because they
        have been tested separately in email service, etc. The validatePasswordResetToken() it's tested here as well
        for valid token.
    */
    @Test
    void shouldResetPassword() {
        //Arrange
        User user = generateUser();
        Integer userId = userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("token");
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(3);
        PasswordResetToken passwordResetToken = new PasswordResetToken(
                userId,
                hashedToken,
                expiryDate);

        passwordResetRepository.createToken(passwordResetToken);

        PasswordResetConfirmationRequest passwordResetConfirmationRequest = new PasswordResetConfirmationRequest(
                "token",
                "3frMH4v!20d4");

        //Act
        underTest.resetPassword(passwordResetConfirmationRequest);

        //Assert
        assertThat(passwordResetRepository.findToken(hashedToken)).isNotPresent();
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
