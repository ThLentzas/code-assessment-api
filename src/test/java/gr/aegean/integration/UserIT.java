package gr.aegean.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import gr.aegean.AbstractIntegrationTest;
import gr.aegean.model.dto.auth.AuthResponse;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/*
    We test the happy paths in ITs.
 */
@AutoConfigureWebTestClient
class UserIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private static final String USER_PATH = "/api/v1/user";
    private final String AUTH_PATH = "/api/v1/auth";
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"));

    @Test
    void shouldGetUser() {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": "Igw4UQAlfX$E"
                }
                """;

        AuthResponse response = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        String jwtToken = response.token();

        webTestClient.get()
                .uri(USER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstname").isEqualTo("Test")
                .jsonPath("$.lastname").isEqualTo("Test")
                .jsonPath("$.username").isEqualTo("TestT");
    }

    @Test
    void shouldGetUserProfile() {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": "Igw4UQAlfX$E"
                }
                """;

        AuthResponse response = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        String jwtToken = response.token();

        webTestClient.get()
                .uri(USER_PATH + "/profile")
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstname").isEqualTo("Test")
                .jsonPath("$.lastname").isEqualTo("Test")
                .jsonPath("$.username").isEqualTo("TestT");
    }

    @Test
    void shouldUpdateUserProfile() {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": "Igw4UQAlfX$E"
                }
                """;

        AuthResponse response = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        String jwtToken = response.token();

        requestBody = """
                {
                     "firstname": "Foo",
                     "lastname": "Foo",
                     "username": "FooBar",
                     "bio": "I like Java",
                     "location": "Miami, OH",
                     "company": "VM, LLC"
                }
                """;

        webTestClient.put()
                .uri(USER_PATH + "/profile")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                .exchange()
                .expectStatus().isNoContent();

        //Get the updated user profile
        webTestClient.get()
                .uri(USER_PATH + "/profile")
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstname").isEqualTo("Foo")
                .jsonPath("$.lastname").isEqualTo("Foo")
                .jsonPath("$.username").isEqualTo("FooBar")
                .jsonPath("$.bio").isEqualTo("I like Java")
                .jsonPath("$.location").isEqualTo("Miami, OH")
                .jsonPath("$.company").isEqualTo("VM, LLC");
    }

    @Test
    void shouldUpdatePassword() {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": "Igw4UQAlfX$E"
                }
                """;

        AuthResponse response = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        String jwtToken = response.token();

        requestBody = """
                {
                    "oldPassword": "Igw4UQAlfX$E",
                    "newPassword": "3frMH4v!20d4"
                }
                """;

        webTestClient.put()
                .uri(USER_PATH + "/settings/password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();

        requestBody = """
                {
                    "email": "test@example.com",
                    "password": "3frMH4v!20d4"
                }
                """;

        //Login the user with the new password
        response = webTestClient.post()
                .uri(AUTH_PATH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response.token()).isNotNull();
    }

    @Test
    void shouldUpdateEmail() throws MessagingException {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": "Igw4UQAlfX$E"
                }
                """;

        AuthResponse response = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        String jwtToken = response.token();

        requestBody = """
                {
                    "email": "foo@example.com",
                    "password": "Igw4UQAlfX$E"
                }
                """;

        webTestClient.post()
                .uri(USER_PATH + "/settings/email")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isAccepted();

        await().atMost(5, TimeUnit.SECONDS).until(() -> greenMail.getReceivedMessages().length == 1);

        MimeMessage[] messages = greenMail.getReceivedMessages();
        MimeMessage message = messages[0];
        String body = GreenMailUtil.getBody(message);

        // Remove encoded line breaks before extracting URL
        body = body.replace("=\r\n", "");
        Pattern pattern = Pattern.compile("http://localhost:8080/api/v1/user/email[^\"]*");
        Matcher matcher = pattern.matcher(body);
        String token = null;

        if (matcher.find()) {
            String url = matcher.group();
            String tokenKey = "token=3D";
            int tokenStartIndex = url.indexOf(tokenKey);

            if (tokenStartIndex != -1) {
                token = url.substring(tokenStartIndex + tokenKey.length());
            }
        }

        message = messages[0];

        assertThat(message.getAllRecipients()).hasSize(1);
        assertThat(message.getAllRecipients()[0]).hasToString("foo@example.com");
        assertThat(message.getSubject()).isEqualTo("Verify your email");

        webTestClient.get()
                .uri(USER_PATH + "/email?token={token}", token)
                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "http://localhost:4200/profile");

        requestBody = """
                {
                    "email": "foo@example.com",
                    "password": "Igw4UQAlfX$E"
                }
                """;

        //Login the user with the new email
        response = webTestClient.post()
                .uri(AUTH_PATH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response.token()).isNotNull();
    }

    @Test
    void shouldDeleteAccount() {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": "Igw4UQAlfX$E"
                }
                """;

        AuthResponse response = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        String jwtToken = response.token();

        requestBody = """
                {
                    "password": "Igw4UQAlfX$E"
                }
                """;

        webTestClient.put()
                .uri(USER_PATH + "/settings/account")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();

        requestBody = """
                {
                    "email": "foo@example.com",
                    "password": "Igw4UQAlfX$E"
                }
                """;

        //Logging in the user will return 401
        webTestClient.post()
                .uri(AUTH_PATH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Username or password is incorrect");
    }
}