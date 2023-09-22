package gr.aegean.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import gr.aegean.entity.User;
import gr.aegean.model.dto.user.UserDTO;
import gr.aegean.model.dto.auth.AuthResponse;
import gr.aegean.model.dto.auth.LoginRequest;
import gr.aegean.model.dto.auth.RegisterRequest;
import gr.aegean.exception.UnauthorizedException;
import gr.aegean.service.user.UserService;
import gr.aegean.model.UserPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    private AuthService underTest;

    @BeforeEach
    void setup() {
        underTest = new AuthService(
                userService,
                passwordEncoder,
                jwtService,
                authenticationManager);
    }

    @Test
    void shouldRegisterUser() {
        //Arrange
        RegisterRequest request = new RegisterRequest(
                "Test",
                "Test",
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr"
        );

        User user = User.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .username(request.username())
                .email(request.email())
                .password(request.password())
                .build();

        String jwtToken = "jwtToken";

        when(passwordEncoder.encode(user.getPassword())).thenReturn("hashedPassword");
        when(jwtService.assignToken(any(UserDTO.class))).thenReturn(jwtToken);

        //Act
        AuthResponse authResponse = underTest.registerUser(request);

        //Assert
        assertThat(authResponse.token()).isEqualTo(jwtToken);

        verify(passwordEncoder, times(1)).encode(user.getPassword());
        verify(userService, times(1)).registerUser(any(User.class));
        verify(jwtService, times(1)).assignToken(any(UserDTO.class));
    }

    /*
        The principal(1st argument) of the UsernamePasswordAuthenticationToken is of type SecurityUser, because
        SecurityUser implements UserDetails.
     */
    @Test
    void shouldAuthenticateUser() {
        //Arrange
        LoginRequest loginRequest = new LoginRequest("test@gmail.com", "test");
        User user = new User(loginRequest.email(), loginRequest.password());

        String jwtToken = "jwtToken";

        when(jwtService.assignToken(any(UserDTO.class))).thenReturn(jwtToken);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null));

        //Act
        AuthResponse authResponse = underTest.loginUser(loginRequest);

        //Assert
        assertThat(authResponse.token()).isEqualTo(jwtToken);

        verify(jwtService, times(1)).assignToken(any(UserDTO.class));
        verify(authenticationManager, times(1)).authenticate(
                any(UsernamePasswordAuthenticationToken.class));
    }

    /*
        When authenticate from authentication manager fails it will throw either spring.security.BadCredentialsException
        if password is wrong or UsernameNotFoundException if the user's email doesn't exist. The
        UsernameNotFoundException is caught by Spring and a spring.security.BadCredentialsException is thrown instead.
     */
    @Test
    void shouldThrowUnauthorizedExceptionWhenAuthEmailOrPasswordIsWrong() {
        //Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Username or password is incorrect"));

        //Act
        LoginRequest request = new LoginRequest("test@example.com", "password");

        //Assert
        assertThatThrownBy(() -> underTest.loginUser(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Username or password is incorrect");

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    }
}
