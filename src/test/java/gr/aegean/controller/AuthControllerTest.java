package gr.aegean.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import gr.aegean.config.security.JwtConfig;
import gr.aegean.config.security.SecurityConfig;
import gr.aegean.config.security.AuthConfig;
import gr.aegean.model.dto.auth.AuthResponse;
import gr.aegean.model.dto.auth.RegisterRequest;
import gr.aegean.model.dto.auth.LoginRequest;
import gr.aegean.service.auth.AuthService;
import gr.aegean.service.auth.PasswordResetService;
import gr.aegean.repository.UserRepository;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    private PasswordResetService passwordResetService;
    @MockBean
    private UserRepository userRepository;
    private static final String AUTH_PATH = "/api/v1/auth";

    @Test
    void shouldReturnJwtTokenAndHTTP201WhenUserIsRegisteredSuccessfully() throws Exception {
        //Arrange
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "Test",
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;
        AuthResponse authResponse = new AuthResponse("jwtToken");

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(authResponse);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is(authResponse.token())));
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
                    "password": "CyN549^*o2Cr"
                }
                """, firstnameValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The First Name field is required")));
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
                    "password": "CyN549^*o2Cr"
                }
                """, lastnameValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Last Name field is required")));
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
                    "password": "CyN549^*o2Cr"
                }
                """, usernameValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .servletPath(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Username field is required")));
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
                    "password": "CyN549^*o2Cr"
                }
                """, emailValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Email field is required")));
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
                    "password": %s
                }
                """, passwordValue);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Password field is required")));
    }

    @Test
    void shouldReturnJwtTokenAndHTTP200WhenUserIsAuthenticatedSuccessfully() throws Exception {
        //Arrange
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;
        AuthResponse authResponse = new AuthResponse("jwtToken");

        when(authService.loginUser(any(LoginRequest.class))).thenReturn(authResponse);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(authResponse.token())));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Email field is necessary")));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Password field is necessary")));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Email field is required")));
    }

    @Test
    void shouldReturnHTTP200ForPasswordResetRequestRegardlessIfEmailExists() throws Exception {
        //Arrange
        String requestBody = """
                {
                    "email": "test@example.com"
                }
                """;

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/password_reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Password field is required")));
    }

    @Test
    void shouldResetPasswordAndReturnHTTP204WhenTokenAndNewPasswordAreValid() throws Exception {
        //Arrange
        String requestBody = """
                {
                    "token": "someToken",
                    "password": "password"
                }
                """;

        //Act Assert
        mockMvc.perform(put(AUTH_PATH + "/password_reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
