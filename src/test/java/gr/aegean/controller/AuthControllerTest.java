package gr.aegean.controller;

import gr.aegean.config.security.SecurityConfig;
import gr.aegean.config.AuthConfig;
import gr.aegean.model.auth.AuthResponse;
import gr.aegean.model.auth.RegisterRequest;
import gr.aegean.model.auth.AuthRequest;
import gr.aegean.model.auth.PasswordResetRequest;
import gr.aegean.model.auth.PasswordResetResponse;
import gr.aegean.service.auth.AuthService;
import gr.aegean.service.auth.CookieService;
import gr.aegean.service.auth.PasswordResetService;
import gr.aegean.repository.UserRepository;
import gr.aegean.config.security.JwtFilter;
import gr.aegean.service.auth.JwtService;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthController.class)
@Import({JwtFilter.class,
        JwtService.class,
        AuthConfig.class,
        SecurityConfig.class})
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AuthService authService;
    @MockBean
    private PasswordResetService passwordResetService;
    @MockBean
    private CookieService cookieService;
    @MockBean
    private UserRepository userRepository;
    private static final String AUTH_PATH = "/api/v1/auth";

    @Test
    void shouldSetJwtTokenInCookieHeaderWhenUserIsRegisteredSuccessfully() throws Exception {
        //Arrange
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "Test",
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }
                """;

        AuthResponse authResponse = new AuthResponse(1, "jwtToken");
        ResponseCookie cookie = ResponseCookie.from("accessToken", authResponse.token())
                .httpOnly(true)
                .secure(false)
                .maxAge(Duration.ofHours(1))
                .sameSite("Lax")
                .build();

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(authResponse);
        when(cookieService.createHttpOnlyCookie(any(String.class))).thenReturn(cookie);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .servletPath(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().value("accessToken", cookie.getValue()))
                .andExpect(header().string("Location", Matchers.containsString(
                        "api/v1/users/" + authResponse.userId())));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenRegisterFirstnameIsNullOrEmpty(String firstname)
            throws Exception {
        //Arrange
        String firstnameValue = firstname == null ? "null" : "\"" + firstname + "\"";
        String requestBody = String.format("""
                {
                    "firstname": %s,
                    "lastname": "Test",
                    "username": "Test",
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }
                """, firstnameValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .servletPath(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The First Name field is required.")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenRegisterLastnameIsNullOrEmpty(String lastname) throws Exception {
        //Arrange
        String lastnameValue = lastname == null ? "null" : "\"" + lastname + "\"";
        String requestBody = String.format("""
                {
                    "firstname": "Test",
                    "lastname": %s,
                    "username": "Test",
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }
                """, lastnameValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .servletPath(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Last Name field is required.")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenRegisterUsernameIsNullOrEmpty(String username) throws Exception {
        //Arrange
        String usernameValue = username == null ? "null" : "\"" + username + "\"";
        String requestBody = String.format("""
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": %s,
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }
                """, usernameValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .servletPath(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Username field is required.")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenRegisterEmailIsNullOrEmpty(String email)
            throws Exception {
        //Arrange
        String emailValue = email == null ? "null" : "\"" + email + "\"";
        String requestBody = String.format("""
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": %s,
                    "password": "CyN549^*o2Cr",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }
                """, emailValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .servletPath(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Email field is required.")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenRegisterPasswordIsNullOrEmpty(String password) throws Exception {
        //Arrange
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": %s,
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }
                """, passwordValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .servletPath(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Password field is required.")));
    }

    @Test
    void shouldSetJwtTokenInCookieHeaderWhenUserIsAuthenticatedSuccessfully() throws Exception {
        //Arrange
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;
        AuthResponse authResponse = new AuthResponse(1, "jwtToken");
        ResponseCookie cookie = ResponseCookie.from("accessToken", authResponse.token())
                .httpOnly(true)
                .secure(false)
                .maxAge(Duration.ofHours(1))
                .sameSite("Lax")
                .build();

        when(authService.authenticateUser(any(AuthRequest.class))).thenReturn(authResponse);
        when(cookieService.createHttpOnlyCookie(any(String.class))).thenReturn(cookie);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/login")
                        .servletPath(AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().value("accessToken", cookie.getValue()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenAuthEmailIsNullOrEmpty(String email) throws Exception {
        //Arrange
        String emailValue = email == null ? "null" : "\"" + email + "\"";
        String requestBody = String.format("""
                {
                    "email": %s,
                    "password": "CyN549^*o2Cr"
                }
                """, emailValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/login")
                        .servletPath(AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("All fields are necessary.")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenAuthPasswordIsNullOrEmpty(String password) throws Exception {
        //Arrange
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "email": "test@example.com",
                    "password": %s
                }
                """, passwordValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/login")
                        .servletPath(AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("All fields are necessary.")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenPasswordResetRequestEmailIsNullOrEmpty(String email)
            throws Exception {
        String emailValue = email == null ? "null" : "\"" + email + "\"";
        String requestBody = String.format("""
                {
                    "email": %s
                }
                """, emailValue);

        mockMvc.perform(post(AUTH_PATH + "/password_reset")
                        .servletPath(AUTH_PATH + "/password_reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Email field is required.")));
    }

    @Test
    void shouldReturnGenericMessageForPasswordResetRequestRegardlessIfEmailExists() throws Exception {
        //Arrange
        String requestBody = """
                {
                    "email": "test@example.com"
                }
                """;
        PasswordResetResponse resetResult = new PasswordResetResponse("If your email address exists in our database, " +
                "you will receive a password recovery link at your email address in a few minutes.");

        when(passwordResetService.createPasswordResetToken(any(PasswordResetRequest.class))).thenReturn(resetResult);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/password_reset")
                        .servletPath(AUTH_PATH + "/password_reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message", is(resetResult.message())));
    }

    @Test
    void shouldReturnHTTP200WhenResetTokenIsValid() throws Exception {
        //Arrange
        String validToken = "token";

        // Act Assert
        mockMvc.perform(get(AUTH_PATH + "/password_reset?token={token}", validToken)
                        .servletPath(AUTH_PATH + "/password_reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
    No need to test the token if its null or empty it is already tested in the validatePasswordResetToken() method
    in PasswordResetServiceTest -> void shouldThrowBadCredentialsExceptionWhenTokenIsInvalid(String invalidToken)
    We test @Valid annotation here
    */
    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnHTTP400WhenNewPasswordIsNullOrEmpty(String newPassword)
            throws Exception {
        //Arrange
        String newPasswordValue = newPassword == null ? "null" : "\"" + newPassword + "\"";
        String requestBody = String.format("""
                {
                    "token": "someToken",
                    "newPassword": %s
                }
                """, newPasswordValue);

        //Act Assert
        mockMvc.perform(put(AUTH_PATH + "/password_reset/confirm")
                        .servletPath(AUTH_PATH + "/password_reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Password field is required.")));
    }

    @Test
    void shouldResetPasswordAndReturnHTTP204WhenTokenAndNewPasswordAreValid() throws Exception {
        //Arrange
        String requestBody = """
                {
                    "token": "someToken",
                    "newPassword": "password"
                }
                """;

        //Act Assert
        mockMvc.perform(put(AUTH_PATH + "/password_reset/confirm")
                        .servletPath(AUTH_PATH + "/password_reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
