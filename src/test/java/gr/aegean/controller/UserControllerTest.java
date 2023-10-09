package gr.aegean.controller;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import gr.aegean.service.auth.AppUserDetailsService;
import gr.aegean.service.auth.JwtService;
import gr.aegean.service.user.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;


/*
    The @Valid annotation is used, so we write tests for invalid inputs(null, empty, other) in the Controller and not
    the Service, that's why we call verifyNoInteractions(userService); in every BadRequest test.
 */
@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class,
        AuthConfig.class,
        AppUserDetailsService.class,
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
        UserDTO userDTO = new UserDTO(
                1,
                "Test",
                "Test",
                "TestT",
                "test@example.com",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );
        String responseBody = """
                {
                    "id": 1,
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }
                """;


        when(userService.findUser()).thenReturn(userDTO);

        mockMvc.perform(get(USER_PATH))
                .andExpectAll(
                        status().isOk(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP404WhenUserIsNotFound() throws Exception {
        String responseBody = """
                {
                    "message": "User not found with id: 1",
                    "statusCode": 404
                }
                """;

        when(userService.findUser()).thenThrow(new ResourceNotFoundException("User not found with id: " + 1));

        mockMvc.perform(get(USER_PATH))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenGetUserIsCalledByUnauthenticatedUser() throws Exception {
        mockMvc.perform(get(USER_PATH))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnUserProfileAndHTTP200() throws Exception {
        UserProfile profile = new UserProfile(
                "Foo",
                "Foo",
                "FooBar",
                "I like Java",
                "Miami, OH",
                "VM, LLC");
        String responseBody = """
                {
                    "firstname": "Foo",
                    "lastname": "Foo",
                    "username": "FooBar",
                    "bio": "I like Java",
                    "location": "Miami, OH",
                    "company": "VM, LLC"
                }
                """;

        when(userService.getProfile()).thenReturn(profile);

        mockMvc.perform(get(USER_PATH + "/profile"))
                .andExpectAll(
                        status().isOk(),
                        content().json(responseBody)
                );
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP404WhenUserIsNotFoundToGetProfile() throws Exception {
        String responseBody = """
                {
                    "message": "User not found with id: 1",
                    "statusCode": 404
                }
                """;
        when(userService.getProfile()).thenThrow(new ResourceNotFoundException("User not found with id: " + 1));

        mockMvc.perform(get(USER_PATH + "/profile"))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenGetProfileIsCalledByUnauthenticatedUser() throws Exception {
        mockMvc.perform(get(USER_PATH + "/profile"))
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

        doNothing().when(userService).updateProfile(any(UserProfileUpdateRequest.class));

        mockMvc.perform(put(USER_PATH + "/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).updateProfile(any(UserProfileUpdateRequest.class));
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
        String responseBody = """
                {
                    "message": "The provided username already exists",
                    "statusCode": 409
                }
                """;

        doThrow(new DuplicateResourceException("The provided username already exists"))
                .when(userService).updateProfile(any(UserProfileUpdateRequest.class));


        mockMvc.perform(put(USER_PATH + "/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
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
        String responseBody = """
                {
                    "message": "User not found with id: 1",
                    "statusCode": 404
                }
                """;

        doThrow(new ResourceNotFoundException("User not found with id: " + 1))
                .when(userService).updateProfile(any(UserProfileUpdateRequest.class));

        mockMvc.perform(put(USER_PATH + "/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
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

        doNothing().when(userService).updatePassword(any(UserPasswordUpdateRequest.class));

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).updatePassword(any(UserPasswordUpdateRequest.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenOldPasswordIsNullOrEmpty(String password) throws Exception {
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "oldPassword": %s,
                    "newPassword": "CyN549^*o2Cr"
                }
                """, passwordValue);
        String responseBody = """
                {
                    "message": "Old password is required",
                    "statusCode": 400
                }
                """;

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenOldPasswordIsWrong() throws Exception {
        String requestBody = """
                {
                    "oldPassword": "wrongPassword",
                    "newPassword": "CyN549^*o2Cr"
                }
                """;
        String responseBody = """
                {
                    "message": "Old password is wrong",
                    "statusCode": 400
                }
                """;

        doThrow(new BadCredentialsException("Old password is wrong"))
                .when(userService).updatePassword(any(UserPasswordUpdateRequest.class));

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenNewPasswordIsNullOrEmpty(String password) throws Exception {
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "oldPassword": "CyN549^*o2Cr",
                    "newPassword": %s
                }
                """, passwordValue);
        String responseBody = """
                {
                    "message": "New password is required",
                    "statusCode": 400
                }
                """;

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenNewPasswordDoesNotMeetTheRequirements() throws Exception {
        String requestBody = """
                {
                    "oldPassword": "CyN549^*o2Cr",
                    "newPassword": "password"
                }
                """;
        String responseBody = """
                {
                    "message": "Password must be 12 or more characters in length.",
                    "statusCode": 400
                }
                """;

        doThrow(new IllegalArgumentException("Password must be 12 or more characters in length."))
                .when(userService).updatePassword(any(UserPasswordUpdateRequest.class));

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
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
        String responseBody = """
                {
                    "message": "User not found with id: 1",
                    "statusCode": 404
                }
                """;

        doThrow(new ResourceNotFoundException("User not found with id: " + 1))
                .when(userService).updatePassword(any(UserPasswordUpdateRequest.class));

        mockMvc.perform(put(USER_PATH + "/settings/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
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

        doNothing().when(userService).createEmailUpdateToken(any(UserEmailUpdateRequest.class));

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(userService, times(1)).createEmailUpdateToken(any(UserEmailUpdateRequest.class));
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
        String responseBody = """
                {
                    "message": "The Password field is required",
                    "statusCode": 400
                }
                """;

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );

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
        String responseBody = """
                {
                    "message": "Wrong password",
                    "statusCode": 400
                }
                """;

        doThrow(new BadCredentialsException("Wrong password"))
                .when(userService).createEmailUpdateToken(any(UserEmailUpdateRequest.class));

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
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
        String responseBody = """
                {
                    "message": "The Email field is required",
                    "statusCode": 400
                }
                """;

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );

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
        String responseBody = """
                {
                    "message": "Email already is use",
                    "statusCode": 409
                }
                """;

        doThrow(new DuplicateResourceException("Email already is use"))
                .when(userService).createEmailUpdateToken(any(UserEmailUpdateRequest.class));

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldUpdateEmailAndRedirectUserToTheirProfileWhenEmailUpdateTokenIsValid() throws Exception {
        String token = "token";

        when(userService.updateEmail(any(String.class))).thenReturn(true);

        mockMvc.perform(get(USER_PATH + "/email?token={token}", token))
                .andExpectAll(
                        status().is3xxRedirection(),
                        header().string("Location", "http://localhost:4200/profile")
                );
    }

    /*
        We consider invalid tokens the following: 1) empty 2) malformed 3) expired. This is the GET endpoint we have as
        permitAll() because the user clicks the verification link from their email
     */
    @Test
    void shouldNotUpdateEmailAndRedirectToEmailUpdateErrorPageWhenEmailUpdateTokenIsInvalid() throws Exception {
        String token = "invalidToken";

        when(userService.updateEmail(any(String.class))).thenReturn(false);

        mockMvc.perform(get(USER_PATH + "/email?token={token}", token))
                .andExpectAll(
                        status().is3xxRedirection(),
                        header().string("Location", "http://localhost:4200/email_update_error")
                );
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
        String responseBody = """
                {
                    "message": "User not found with id: 1",
                    "statusCode": 404
                }
                """;

        doThrow(new ResourceNotFoundException("User not found with id: " + 1))
                .when(userService).createEmailUpdateToken(any(UserEmailUpdateRequest.class));

        mockMvc.perform(post(USER_PATH + "/settings/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody)
                );
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
    @ValueSource(strings = {
            "22-04-2020",
            "04-22-2020"
    })
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenAtLeastOneDateIsInvalidToGetUserHistoryInRange(String from) throws Exception {
        String to = "2022-03-13";
        String fromValue = from == null ? "null" : from;
        String responseBody = String.format("""
                {
                    "message": "The provided date is invalid: %s",
                    "statusCode": 400
                }
                """, fromValue);

        when(userService.getHistory(any(String.class), any(String.class))).thenThrow(
                new IllegalArgumentException(String.format("The provided date is invalid: %s", fromValue)));

        mockMvc.perform(get(USER_PATH + "/history?from={from}&to={to}", from, to))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
    }

    @Test
    void shouldReturnHTTP401WhenGetHistoryIsCalledByUnauthenticatedUser() throws Exception {
        mockMvc.perform(get(USER_PATH + "/history"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP204WhenAccountIsDeleted() throws Exception {
        String requestBody = """
                {
                    "password": "CyN549^*o2Cr"
                }
                """;

        doNothing().when(userService).deleteAccount(any(UserAccountDeleteRequest.class));

        mockMvc.perform(put(USER_PATH + "/settings/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteAccount(any(UserAccountDeleteRequest.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenPasswordIsNullOrEmptyForAccountDeletion(String password) throws Exception {
        String passwordValue = password == null ? "null" : "\"" + password + "\"";
        String requestBody = String.format("""
                {
                    "password": %s
                }
                """, passwordValue);
        String responseBody = """
                {
                    "message": "The Password field is required",
                    "statusCode": 400
                }
                """;

        mockMvc.perform(put(USER_PATH + "/settings/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenPasswordIsWrongForAccountDeletion() throws Exception {
        String requestBody = """
                {
                    "password": "wrongPassword"
                }
                """;
        String responseBody = """
                {
                    "message": "Password is wrong",
                    "statusCode": 400
                }
                """;

        doThrow(new BadCredentialsException("Password is wrong"))
                .when(userService).deleteAccount(any(UserAccountDeleteRequest.class));

        mockMvc.perform(put(USER_PATH + "/settings/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody)
                );
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
