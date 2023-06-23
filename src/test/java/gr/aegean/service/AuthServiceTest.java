package gr.aegean.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import gr.aegean.model.user.User;
import gr.aegean.mapper.UserDTOMapper;
import gr.aegean.model.user.UserDTO;
import gr.aegean.security.auth.AuthResponse;
import gr.aegean.security.auth.AuthRequest;
import gr.aegean.security.auth.RegisterRequest;
import gr.aegean.exception.UnauthorizedException;
import gr.aegean.exception.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;

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
    private final UserDTOMapper userDTOMapper = new UserDTOMapper();
    private AuthService underTest;

    @BeforeEach
    void setup() {
        underTest = new AuthService(
                userService,
                passwordEncoder,
                jwtService,
                authenticationManager,
                userDTOMapper);
    }

    @Test
    void shouldRegisterUser() {
        //Arrange
        RegisterRequest request = new RegisterRequest(
                "Test",
                "Test",
                "TestT",
                "test@gmail.com",
                "CyN549^*o2Cr",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        User user = new User(
                request.firstname(),
                request.lastname(),
                request.username(),
                request.email(),
                request.password(),
                request.bio(),
                request.location(),
                request.company());

        String jwtToken = "jwtToken";
        Integer generatedID = 1;

        when(passwordEncoder.encode(user.getPassword())).thenReturn("hashedPassword");
        when(userService.registerUser(any(User.class))).thenReturn(generatedID);
        when(jwtService.assignToken(any(UserDTO.class))).thenReturn(jwtToken);

        //Act
        AuthResponse authResponse = underTest.register(request);

        //Assert
        assertThat(authResponse.getId()).isEqualTo(generatedID);
        assertThat(authResponse.getToken()).isEqualTo(jwtToken);

        verify(passwordEncoder, times(1)).encode(user.getPassword());
        verify(userService, times(1)).registerUser(any(User.class));
        verify(jwtService, times(1)).assignToken(any(UserDTO.class));
    }

    @Test
    void shouldAuthenticateUser() {
        //Arrange
        AuthRequest authRequest = new AuthRequest("test@gmail.com", "test");
        User user = new User(authRequest.email(), authRequest.password());

        String jwtToken = "jwtToken";

        when(jwtService.assignToken(any(UserDTO.class))).thenReturn(jwtToken);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(
                new UsernamePasswordAuthenticationToken(user, "test"));

        //Act
        AuthResponse authResponse = underTest.authenticate(authRequest);

        //Assert
        assertThat(authResponse.getToken()).isEqualTo(jwtToken);

        verify(jwtService, times(1)).assignToken(any(UserDTO.class));
        verify(authenticationManager, times(1)).authenticate(
                any(UsernamePasswordAuthenticationToken.class));
    }

    //When authenticate from authentication manager fails it will throw either spring.security.BadCredentialsException
    //if password is wrong or EmptyResultDataAccessException if the user's email doesn't exist. In any case both are run
    //time exceptions.
    @Test
    void shouldThrowBadCredentialsExceptionWhenAuthEmailOrPasswordIsWrong() {
        //Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException());

        //Act
        AuthRequest request = new AuthRequest("test@example.com", "password");

        //Assert
        assertThatThrownBy(() -> underTest.authenticate(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Username or password is incorrect");

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    }
}
