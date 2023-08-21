package gr.aegean.controller;

import gr.aegean.config.security.JwtFilter;
import gr.aegean.exception.UnauthorizedException;
import gr.aegean.service.auth.CookieService;
import gr.aegean.service.auth.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

import gr.aegean.config.AuthConfig;
import gr.aegean.config.security.SecurityConfig;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.model.user.UserUpdateEmailRequest;
import gr.aegean.model.user.UserProfile;
import gr.aegean.model.user.UserProfileUpdateRequest;
import gr.aegean.repository.UserRepository;
import gr.aegean.service.user.UserService;


@WebMvcTest(UserController.class)
@Import({JwtFilter.class,
        JwtService.class,
        AuthConfig.class,
        SecurityConfig.class})
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserService userService;
    @MockBean
    private CookieService cookieService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserRepository userRepository;
    private static final String USER_PATH = "/api/v1/user";

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP204WhenProfileIsUpdatedForAuthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "firstname": "Foo",
                    "lastname": "Foo",
                    "username": "FooBar",
                    "bio": "I like Java",
                    "location": "Miami, OH",
                    "company": "VM, LLC"
                }
                """;

        mockMvc.perform(put(USER_PATH + "/settings/profile")
                        .servletPath(USER_PATH + "/settings/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP409WhenUpdatingProfileWithExistingUsername() throws Exception {
        String requestBody = """
                {
                    "firstname": "Foo",
                    "lastname": "Foo",
                    "username": "FooBar",
                    "bio": "I like Java",
                    "location": "Miami, OH",
                    "company": "VM, LLC"
                }
                """;

        doThrow(new DuplicateResourceException("The provided username already exists"))
                .when(userService).updateProfile(any(HttpServletRequest.class), any(UserProfileUpdateRequest.class));


        mockMvc.perform(put(USER_PATH + "/settings/profile")
                        .servletPath(USER_PATH + "/settings/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnHTTP401WhenUpdateProfileIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "firstname": "Foo",
                    "lastname": "Foo",
                    "username": "FooBar",
                    "bio": "I like Java",
                    "location": "Miami, OH",
                    "company": "VM, LLC"
                }
                """;

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenThrow(
                new UnauthorizedException("Unauthorized"));

        mockMvc.perform(put(USER_PATH + "/settings/profile")
                        .servletPath(USER_PATH + "/settings/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP204WhenPasswordIsUpdatedForAuthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "oldPassword": "3frMH4v!20d4",
                    "updatedPassword": "CyN549^*o2Cr"
                }
                """;

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .servletPath(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnHTTP401WhenUpdatePasswordIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "oldPassword": "3frMH4v!20d4",
                    "updatedPassword": "CyN549^*o2Cr"
                }
                """;

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenThrow(
                new UnauthorizedException("Unauthorized"));

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .servletPath(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenOldPasswordIsNullOrEmpty(String password) throws Exception {
        //Arrange
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "oldPassword": %s,
                    "updatedPassword": "CyN549^*o2Cr"
                }

                """, passwordValue);

        //Act Assert
        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenNewPasswordIsNullOrEmpty(String password) throws Exception {
        //Arrange
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "oldPassword": %s,
                    "updatedPassword": "CyN549^*o2Cr"
                }
                """, passwordValue);

        //Act Assert
        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .servletPath(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnProfileAndHTTP200WhenUserIsAuthenticated() throws Exception {
        //Arrange
        UserProfile profile = new UserProfile(
                "Foo",
                "Foo",
                "FooBar",
                "I like Java",
                "Miami, OH",
                "VM, LLC"
        );

        //Act
        when(userService.getProfile(any(HttpServletRequest.class))).thenReturn(profile);

        //Assert
        mockMvc.perform(get(USER_PATH + "/profile")
                        .servletPath(USER_PATH + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstname", is(profile.firstname())))
                .andExpect(jsonPath("$.lastname", is(profile.lastname())))
                .andExpect(jsonPath("$.username", is(profile.username())))
                .andExpect(jsonPath("$.bio", is(profile.bio())))
                .andExpect(jsonPath("$.location", is(profile.location())))
                .andExpect(jsonPath("$.company", is(profile.company())));
    }

    @Test
    void shouldReturnHTTP401WhenGetProfileIsCalledByUnauthenticatedUser() throws Exception {
        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenThrow(
                new UnauthorizedException("Unauthorized"));

        mockMvc.perform(get(USER_PATH + "/profile")
                        .servletPath(USER_PATH + "/profile"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP202WhenEmailIsUpdatedForAuthenticated() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .servletPath(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldReturnHTTP401WhenUpdateEmailIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenThrow(
                new UnauthorizedException("Unauthorized"));

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .servletPath(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturn400WhenIncorrectPasswordProvidedForEmailUpdate() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "wrongPassword"
                }
                """;

        doThrow(new BadCredentialsException("Wrong password"))
                .when(userService).createEmailUpdateToken(any(HttpServletRequest.class), any(UserUpdateEmailRequest.class));


        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .servletPath(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP409WhenUpdatingEmailWithExistingEmail() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;

        doThrow(new DuplicateResourceException("Email already is use"))
                .when(userService).createEmailUpdateToken(any(HttpServletRequest.class), any(UserUpdateEmailRequest.class));


        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .servletPath(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenPasswordProvidedIsNullOrEmptyForEmailUpdate(String password) throws Exception {
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "email": "test@example.com",
                    "password": %s
                }
                """, passwordValue);

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .servletPath(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenEmailProvidedIsNullOrEmptyForEmailUpdate(String email) throws Exception {
        String emailValue = email == null ? "null" : "\"" + email + "\"";
        String requestBody = String.format("""
                {
                    "email": %s,
                    "password": "password"
                }
                """, emailValue);

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .servletPath(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturnHTTP401WhenGetHistoryIsCalledByUnauthenticatedUser() throws Exception {
        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenThrow(
                new UnauthorizedException("Unauthorized"));

        mockMvc.perform(post(USER_PATH + "/history")
                        .servletPath(USER_PATH + "/history"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }
}
