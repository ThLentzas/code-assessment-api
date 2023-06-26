package gr.aegean.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import gr.aegean.config.JwtConfig;
import gr.aegean.config.SecurityConfig;
import gr.aegean.config.AuthConfig;
import gr.aegean.model.auth.AuthResponse;
import gr.aegean.model.auth.RegisterRequest;
import gr.aegean.model.auth.AuthRequest;
import gr.aegean.model.passwordreset.PasswordResetRequest;
import gr.aegean.model.passwordreset.PasswordResetResult;
import gr.aegean.service.AuthService;
import gr.aegean.service.PasswordResetService;
import gr.aegean.repository.UserRepository;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class,
        AuthConfig.class,
        JwtConfig.class})
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AuthService authService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PasswordResetService passwordResetService;

    private static final String AUTH_PATH = "/api/v1/auth";

    @Test
    void shouldReturnJwtTokenWhenUserIsRegisteredSuccessfully() throws Exception {
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
        AuthResponse authResponse = new AuthResponse("jwtToken", 1);

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(authResponse);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString(
                        "api/v1/users/" + authResponse.getId())))
                .andExpect(jsonPath("$.token", is(authResponse.getToken())));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path", is("/api/v1/auth/signup")))
                .andExpect(jsonPath("$.message", is("The First Name field is required.")));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path", is("/api/v1/auth/signup")))
                .andExpect(jsonPath("$.message", is("The Last Name field is required.")));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path", is("/api/v1/auth/signup")))
                .andExpect(jsonPath("$.message", is("The Username field is required.")));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path", is("/api/v1/auth/signup")))
                .andExpect(jsonPath("$.message", is("The Email field is required.")));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path", is("/api/v1/auth/signup")))
                .andExpect(jsonPath("$.message", is("The Password field is required.")));
    }

    @Test
    void shouldReturnJwtTokenWhenUserIsAuthenticatedSuccessfully() throws Exception {
        //Arrange
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;
        AuthResponse authResponse = new AuthResponse("jwtToken");

        when(authService.authenticateUser(any(AuthRequest.class))).thenReturn(authResponse);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(authResponse.getToken())));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path", is("/api/v1/auth/login")))
                .andExpect(jsonPath("$.message", is("All fields are necessary.")));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path", is("/api/v1/auth/login")))
                .andExpect(jsonPath("$.message", is("All fields are necessary.")));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void shouldReturnHTTP400WhenPasswordResetRequestEmailIsNullOrEmpty(String email)
            throws Exception{
        String emailValue = email == null ? "null" : "\"" + email + "\"";
        String requestBody = String.format("""
                {
                    "email": %s
                }
                """, emailValue);

        mockMvc.perform(post(AUTH_PATH + "/password_reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path", is("/api/v1/auth/password_reset")))
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
        PasswordResetResult resetResult = new PasswordResetResult("If your email address exists in our database, " +
                "you will receive a password recovery link at your email address in a few minutes.");

        when(passwordResetService.createPasswordResetToken(any(PasswordResetRequest.class))).thenReturn(resetResult);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/password_reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(resetResult.message())));
    }

    @Test
    void shouldReturnHTTP200WhenResetTokenIsValid() throws Exception {
        //Arrange
        String validToken = "token";

        // Act Assert
        mockMvc.perform(get(AUTH_PATH + "/password_reset?token={token}", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void shouldResetPasswordWhenTokenAndUpdatedPasswordAreValid() throws Exception {
        //Arrange
        String requestBody = """
            {
                "token": "someToken",
                "updatedPassword": "updatedPassword"
            }
            """;

        //Act Assert
        mockMvc.perform(put(AUTH_PATH + "/password_reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /*
        No need to test the token if its null or empty it is already tested in the validatePasswordResetToken() method
        in PasswordResetServiceTest class void shouldThrowBadCredentialsExceptionWhenTokenIsInvalid(String invalidToken)
     */
    @ParameterizedTest
    @NullSource
    @EmptySource
    void shouldReturnHTTP400WhenUpdatedPasswordIsNullOrEmpty(String updatedPassword)
            throws Exception {
        //Arrange
        String updatedPasswordValue = updatedPassword == null ? "null" : "\"" + updatedPassword + "\"";
        String requestBody = String.format("""
            {
                "token": "someToken",
                "updatedPassword": %s
            }
            """, updatedPasswordValue);

        //Act Assert
        mockMvc.perform(put(AUTH_PATH + "/password_reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path", is("/api/v1/auth/password_reset/confirm")))
                .andExpect(jsonPath("$.message", is("The Password field is required.")));
    }
}
