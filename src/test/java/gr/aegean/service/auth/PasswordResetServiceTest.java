package gr.aegean.service.auth;

import gr.aegean.entity.User;
import gr.aegean.model.dto.auth.PasswordResetRequest;
import gr.aegean.entity.PasswordResetToken;
import gr.aegean.model.dto.auth.PasswordResetConfirmationRequest;
import gr.aegean.repository.UserRepository;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.utility.StringUtils;
import gr.aegean.AbstractUnitTest;
import gr.aegean.service.email.EmailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;


@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest extends AbstractUnitTest {
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

        userRepository.deleteAllUsers();
    }

    @Test
    void shouldCreatePasswordResetTokenWhenUserIsFound() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);

        PasswordResetRequest passwordResetRequest = new PasswordResetRequest("test@example.com");

        //Act
        underTest.createPasswordResetToken(passwordResetRequest);

        //Assert
        verify(emailService, times(1)).sendPasswordResetEmail(
                eq(user.getEmail()),
                any(String.class));
    }

    @Test
    void shouldNotCreatePasswordResetTokenWhenUserIsNotFound() {
        PasswordResetRequest passwordResetRequest = new PasswordResetRequest("test1@example.com");

        //Act
        underTest.createPasswordResetToken(passwordResetRequest);

        //Assert
        verifyNoInteractions(emailService);
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenPasswordResetTokenIsInvalid() {
        //Arrange
        PasswordResetConfirmationRequest passwordResetConfirmationRequest = new PasswordResetConfirmationRequest(
                "invalidToken",
                "@4ts0v6$Cz06");

        // Act Assert
        assertThatThrownBy(() -> underTest.resetPassword(passwordResetConfirmationRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Reset password token is invalid");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenPasswordResetTokenExpired() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("expiredToken");
        LocalDateTime expiryDate = LocalDateTime.now().minusHours(1);
        PasswordResetToken passwordResetToken = new PasswordResetToken(
                user.getId(),
                hashedToken,
                expiryDate);
        PasswordResetConfirmationRequest passwordResetConfirmationRequest = new PasswordResetConfirmationRequest(
                "expiredToken",
                "@4ts0v6$Cz06");

        passwordResetRepository.saveToken(passwordResetToken);

        //Act Assert
        assertThatThrownBy(() -> underTest.resetPassword(passwordResetConfirmationRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("The password reset link has expired. Please request a new one");
    }

    @Test
    void shouldInvalidateAllPreviousTokensWhenNewResetTokenIsGenerated() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("token");
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(3);
        PasswordResetToken passwordResetToken = new PasswordResetToken(
                user.getId(),
                hashedToken,
                expiryDate);

        passwordResetRepository.saveToken(passwordResetToken);

        PasswordResetRequest passwordResetRequest = new PasswordResetRequest("test@example.com");

        //Act
        underTest.createPasswordResetToken(passwordResetRequest);

        //Assert
        assertThat(passwordResetRepository.findToken(hashedToken)).isNotPresent();
    }

    /*
        No need to test for the password encoder or to validate the updated password or the email service because they
        have been tested separately in email service, etc.
    */
    @Test
    void shouldResetPassword() {
        //Arrange
        User user = generateUser();
        userRepository.registerUser(user);

        String hashedToken = StringUtils.hashToken("token");
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(3);
        PasswordResetToken passwordResetToken = new PasswordResetToken(
                user.getId(),
                hashedToken,
                expiryDate);

        passwordResetRepository.saveToken(passwordResetToken);

        PasswordResetConfirmationRequest passwordResetConfirmationRequest = new PasswordResetConfirmationRequest(
                "token",
                "@4ts0v6$Cz06");

        //Act
        underTest.resetPassword(passwordResetConfirmationRequest);

        //Assert
        assertThat(passwordResetRepository.findToken(hashedToken)).isNotPresent();
        verify(emailService, times(1)).sendPasswordResetSuccessEmail(
                user.getEmail(),
                user.getUsername());
    }

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

}
