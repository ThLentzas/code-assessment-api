package gr.aegean.controller;

import gr.aegean.config.AuthConfig;
import gr.aegean.config.JwtConfig;
import gr.aegean.config.SecurityConfig;
import gr.aegean.model.user.UserProfile;
import gr.aegean.repository.UserRepository;
import gr.aegean.service.UserService;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class,
        AuthConfig.class,
        JwtConfig.class})
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserService userService;
    @MockBean
    private UserRepository userRepository;
    private static final String USER_PATH = "/api/v1/users";

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP204WhenUserProfileIsUpdatedForAuthenticatedUser() throws Exception {
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

        mockMvc.perform(put(USER_PATH + "/{userId}/settings/profile", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnHTTP401WhenUpdateUserProfileIsCalledByUnauthenticatedUser() throws Exception{
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

        mockMvc.perform(put(USER_PATH + "/{userId}/settings/profile", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP204WhenUserPasswordIsUpdatedForAuthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "oldPassword": "3frMH4v!20d4",
                    "updatedPassword": "CyN549^*o2Cr"
                }
                """;

        mockMvc.perform(put(USER_PATH + "/{userId}/settings/password", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnHTTP401WhenUpdateUserPasswordIsCalledByUnauthenticatedUser() throws Exception{
        String requestBody = """
                {
                    "oldPassword": "3frMH4v!20d4",
                    "updatedPassword": "CyN549^*o2Cr"
                }
                """;

        mockMvc.perform(put(USER_PATH + "/{userId}/settings/password", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
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
        mockMvc.perform(put(USER_PATH + "/{userId}/settings/password", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
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
        mockMvc.perform(put(USER_PATH + "/{userId}/settings/password", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnUserProfileWhenUserIsAuthenticated() throws Exception {
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
        when(userService.getProfile(any(Integer.class))).thenReturn(profile);

        //Assert
        mockMvc.perform(get(USER_PATH + "/{userId}/profile", 1))
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
        mockMvc.perform(get(USER_PATH + "/{userId}/profile", 1))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }
}
