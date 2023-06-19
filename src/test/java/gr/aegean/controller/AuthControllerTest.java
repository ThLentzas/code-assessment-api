package gr.aegean.controller;

import gr.aegean.config.AuthConfig;
import gr.aegean.repository.UserRepository;
import gr.aegean.security.auth.AuthRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import gr.aegean.config.JwtConfig;
import gr.aegean.config.SecurityConfig;
import gr.aegean.security.auth.AuthResponse;
import gr.aegean.security.auth.RegisterRequest;
import gr.aegean.service.AuthService;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@WebMvcTest
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

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("api/v1/users/" + 1)))
                .andExpect(jsonPath("$.token", is("jwtToken")));
    }

    @Test
    void shouldReturnJwtTokenWhenUserIsAuthenticatedSuccessfully() throws Exception {
        //Arrange
        String requestBody = """
                {
                    "email": "tl@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;
        AuthResponse authResponse = new AuthResponse("jwtToken");

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(authResponse);

        //Act Assert
        mockMvc.perform(post(AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwtToken")));
    }
}
