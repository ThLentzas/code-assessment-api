package gr.aegean.controller;

import gr.aegean.config.security.AuthConfig;
import gr.aegean.config.security.JwtConfig;
import gr.aegean.config.security.SecurityConfig;
import gr.aegean.exception.DuplicateResourceException;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.model.dto.user.UserAccountDeleteRequest;
import gr.aegean.model.dto.user.UserDTO;
import gr.aegean.model.dto.user.UserEmailUpdateRequest;
import gr.aegean.model.dto.user.UserPasswordUpdateRequest;
import gr.aegean.model.dto.user.UserProfile;
import gr.aegean.model.dto.user.UserProfileUpdateRequest;
import gr.aegean.repository.UserRepository;
import gr.aegean.service.user.UserService;
import gr.aegean.service.auth.JwtService;

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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;


/*
    The @Valid annotation is used, so we write tests for invalid inputs(null, empty, other) in the Controller and not
    the Service, that's why we call verifyNoInteractions(userService); in every BadRequest test.
 */
@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class,
        AuthConfig.class,
        JwtConfig.class})
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserService userService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserRepository userRepository;
    private static final String USER_PATH = "/api/v1/user";

    @Test
    @WithMockUser(username = "test")
    void shouldReturnUserDTOAndHTTP200() throws Exception {
        UserDTO actual = new UserDTO(
                1,
                "Test",
                "Test",
                "TestT",
                "test@example.com",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        when(userService.findUser()).thenReturn(actual);

        mockMvc.perform(get(USER_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(actual.id())))
                .andExpect(jsonPath("$.firstname", is(actual.firstname())))
                .andExpect(jsonPath("$.lastname", is(actual.lastname())))
                .andExpect(jsonPath("$.username", is(actual.username())))
                .andExpect(jsonPath("$.email", is(actual.email())))
                .andExpect(jsonPath("$.bio", is(actual.bio())))
                .andExpect(jsonPath("$.location", is(actual.location())))
                .andExpect(jsonPath("$.company", is(actual.company())));
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP404WhenUserIsNotFound() throws Exception {
        when(userService.findUser()).thenThrow(new ResourceNotFoundException("User not found with id: " + 1));

        mockMvc.perform(get(USER_PATH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("User not found with id: " + 1)));
    }

    @Test
    void shouldReturnHTTP401WhenGetUserIsCalledByUnauthenticatedUser() throws Exception {
        mockMvc.perform(get(USER_PATH))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP204WhenProfileIsUpdated() throws Exception {
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

        mockMvc.perform(put(USER_PATH + "/profile")
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
                .when(userService).updateProfile(any(UserProfileUpdateRequest.class));


        mockMvc.perform(put(USER_PATH + "/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("The provided username already exists")));
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP404WhenUserIsNotFoundToUpdateProfile() throws Exception {
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

        doThrow(new ResourceNotFoundException("User not found with id: " + 1))
                .when(userService).updateProfile(any(UserProfileUpdateRequest.class));

        mockMvc.perform(put(USER_PATH + "/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("User not found with id: " + 1)));
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

        mockMvc.perform(put(USER_PATH + "/settings/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP204WhenPasswordIsUpdated() throws Exception {
        String requestBody = """
                {
                    "oldPassword": "3frMH4v!20d4",
                    "newPassword": "Igw4UQAlfX$E"
                }
                """;

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP404WhenUserIsNotFoundToUpdatePassword() throws Exception {
        String requestBody = """
                {
                    "oldPassword": "3frMH4v!20d4",
                    "newPassword": "Igw4UQAlfX$E"
                }
                """;

        doThrow(new ResourceNotFoundException("User not found with id: " + 1))
                .when(userService).updatePassword(any(UserPasswordUpdateRequest.class));

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("User not found with id: " + 1)));
    }

    @Test
    void shouldReturnHTTP401WhenUpdatePasswordIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "oldPassword": "3frMH4v!20d4",
                    "newPassword": "Igw4UQAlfX$E"
                }
                """;

        mockMvc.perform(put(USER_PATH + "/settings/password")
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
                    "newPassword": "CyN549^*o2Cr"
                }
                """, passwordValue);

        //Act Assert
        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Old password is required")));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenOldPasswordIsWrong() throws Exception{
        String requestBody = """
                {
                    "oldPassword": "wrongPassword",
                    "newPassword": "CyN549^*o2Cr"
                }
                """;

        doThrow(new BadCredentialsException("Old password is wrong"))
                .when(userService).updatePassword(any(UserPasswordUpdateRequest.class));

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Old password is wrong")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenNewPasswordIsNullOrEmpty(String password) throws Exception {
        //Arrange
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "oldPassword": "CyN549^*o2Cr",
                    "newPassword": %s
                }
                """, passwordValue);

        //Act Assert
        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("New password is required")));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenNewPasswordDoesNotMeetTheRequirements() throws Exception{
        String requestBody = """
                {
                    "oldPassword": "CyN549^*o2Cr",
                    "newPassword": "password"
                }
                """;

        doThrow(new IllegalArgumentException("Password must be 12 or more characters in length."))
                .when(userService).updatePassword(any(UserPasswordUpdateRequest.class));

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Password must be 12 or more characters in length.")));
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnProfileAndHTTP200() throws Exception {
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
        when(userService.getProfile()).thenReturn(profile);

        //Assert
        mockMvc.perform(get(USER_PATH + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstname", is(profile.firstname())))
                .andExpect(jsonPath("$.lastname", is(profile.lastname())))
                .andExpect(jsonPath("$.username", is(profile.username())))
                .andExpect(jsonPath("$.bio", is(profile.bio())))
                .andExpect(jsonPath("$.location", is(profile.location())))
                .andExpect(jsonPath("$.company", is(profile.company())));
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP404WhenUserIsNotFoundToGetProfile() throws Exception {
        //Arrange
        when(userService.getProfile()).thenThrow(new ResourceNotFoundException("User not found with id: " + 1));

        //Act Assert
        mockMvc.perform(get(USER_PATH + "/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnHTTP401WhenGetProfileIsCalledByUnauthenticatedUser() throws Exception {
        //Arrange Act Assert
        mockMvc.perform(get(USER_PATH + "/profile"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    /*
        This is the endpoint where the email token is created and then added in the verification link that is sent
        to the user's new email.
     */
    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP202WhenEmailTokenIsCreatedForEmailUpdate() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP404WhenUserIsNotFoundToUpdateEmail() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;

        doThrow(new ResourceNotFoundException("User not found with id: " + 1))
                .when(userService).createEmailUpdateToken(any(UserEmailUpdateRequest.class));

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("User not found with id: " + 1)));
    }

    @Test
    void shouldReturnHTTP401WhenUpdateEmailIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenPasswordIsNullOrEmptyForEmailUpdate(String password) throws Exception {
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "email": "test@example.com",
                    "password": %s
                }
                """, passwordValue);

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Password field is required")));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenWrongPasswordIsProvidedForEmailUpdate() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "wrongPassword"
                }
                """;

        doThrow(new BadCredentialsException("Wrong password"))
                .when(userService).createEmailUpdateToken(any(UserEmailUpdateRequest.class));

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Wrong password")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenEmailIsNullOrEmptyForEmailUpdate(String email) throws Exception {
        String emailValue = email == null ? "null" : "\"" + email + "\"";
        String requestBody = String.format("""
                {
                    "email": %s,
                    "password": "password"
                }
                """, emailValue);

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Email field is required")));

        verifyNoInteractions(userService);
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
                .when(userService).createEmailUpdateToken(any(UserEmailUpdateRequest.class));

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Email already is use")));
    }

    @Test
    void shouldReturnHTTP401WhenGetHistoryIsCalledByUnauthenticatedUser() throws Exception {
        mockMvc.perform(post(USER_PATH + "/history"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP204WhenAccountIsDeleted() throws Exception{
        String requestBody = """
                {
                    "password": "CyN549^*o2Cr"
                }
                """;

        mockMvc.perform(put(USER_PATH + "/settings/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenPasswordIsNullOrEmptyForAccountDeletion(String password) throws Exception{
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "password": %s
                }
                """, passwordValue);

        mockMvc.perform(put(USER_PATH + "/settings/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The Password field is required")));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenPasswordIsWrongForAccountDeletion() throws Exception{
        String requestBody = """
                {
                    "password": "wrongPassword"
                }
                """;

        doThrow(new BadCredentialsException("Password is wrong"))
                .when(userService).deleteAccount(any(UserAccountDeleteRequest.class));

        mockMvc.perform(put(USER_PATH + "/settings/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Password is wrong")));
    }

    @Test
    void shouldReturnHTTP401WhenDeleteAccountIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "password": "CyN549^*o2Cr"
                }
                """;

        mockMvc.perform(put(USER_PATH + "/settings/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }
}
