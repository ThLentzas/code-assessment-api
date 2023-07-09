package gr.aegean.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.aegean.AbstractIntegrationTest;
import gr.aegean.model.auth.AuthResponse;


@AutoConfigureWebTestClient
class AuthIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private final String AUTH_PATH = "/api/v1/auth";
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"));

    @Test
    void shouldLoginUser() {
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
                }""";

        String jwtToken = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody()
                .getToken();

        assertThat(jwtToken).isNotNull();

        requestBody = """
                {
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr"
                }""";

        jwtToken = webTestClient.post()
                .uri(AUTH_PATH + "/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody()
                .getToken();

        assertThat(jwtToken).isNotNull();
    }

    @Test
    void shouldNotLoginUserWhenPasswordIsWrong() {
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
                }""";

        String jwtToken = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody()
                .getToken();

        assertThat(jwtToken).isNotNull();

        requestBody = """
                {
                    "email": "test@example.com",
                    "password": "wrongPassword"
                }""";

        webTestClient.post()
                .uri(AUTH_PATH + "/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error", is("Username or password is incorrect"));;
    }

    @Test
    void shouldNotLoginUserWhenEmailIsWrong() {
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
                }""";

        String jwtToken = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody()
                .getToken();

        assertThat(jwtToken).isNotNull();

        requestBody = """
                {
                    "email": "wrongemail@example.com",
                    "password": "password"
                }""";

        webTestClient.post()
                .uri(AUTH_PATH + "/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error", is("Username or password is incorrect"));
    }

    @Test
    void shouldResetPassword() throws MessagingException {
        // Create a user
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
                }""";

        String jwtToken = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody()
                .getToken();

        assertThat(jwtToken).isNotNull();

        requestBody = """
                {
                    "email": "test@example.com"
                }
                """;

        // Sending the email containing the password reset token on the body.
        webTestClient.post()
                .uri(AUTH_PATH + "/password_reset")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.message", is("If your email address exists in our database, you will " +
                        "receive a password recovery link at your email address in a few minutes."));

        /*
          Extracting the token from the send email to be used in the GET request. The url part is encoded since we
          can't click it so our browser can do the decoding we have to extract the token from the encoded url for
          testing only. This process is down by our browser.
         */
        MimeMessage[] messages = greenMail.getReceivedMessages();
        MimeMessage message = messages[0];
        String body = GreenMailUtil.getBody(message);

        // Remove encoded line breaks before extracting URL
        body = body.replace("=\r\n", "");
        Pattern pattern = Pattern.compile("http://localhost:8080/api/v1/auth/password_reset[^\"]*");
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

        //Assertions for the password reset link email
        assertThat(message.getAllRecipients()).hasSize(1);
        assertThat(message.getAllRecipients()[0]).hasToString("test@example.com");
        assertThat(message.getSubject()).isEqualTo("Reset your Jarvis password");

        // The user if the password reset token is valid will be redirected by the front end
        webTestClient.get()
                .uri(AUTH_PATH + "/password_reset?token={token}", token)
                .exchange()
                .expectStatus().isOk();

        requestBody = String.format("""
            {
                "token": "%s",
                "newPassword": "3frMH4v!20d4"
            }""", token);

        webTestClient.put()
                .uri(AUTH_PATH + "/password_reset/confirm")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();

        messages = greenMail.getReceivedMessages();
        message = messages[1];

        //Assertions for the password reset confirmation email
        assertThat(message.getAllRecipients()).hasSize(1);
        assertThat(message.getAllRecipients()[0]).hasToString("test@example.com");
        assertThat(message.getSubject()).isEqualTo("Your password was reset");
    }

    @Test
    void shouldNotResetPasswordWhenEmailDoesNotExist() {
        // Create a user
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "Test",
                    "email": "greenmail@example.com",
                    "password": "CyN549^*o2Cr",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }""";

        String jwtToken = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody()
                .getToken();

        assertThat(jwtToken).isNotNull();

        requestBody = """
                {
                    "email": "nonexisting@gmail.com"
                }
                """;

        // Sending the email containing the password reset token on the body.
        webTestClient.post()
                .uri(AUTH_PATH + "/password_reset")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.message", is("If your email address exists in our database, you will " +
                        "receive a password recovery link at your email address in a few minutes."));

        MimeMessage[] messages = greenMail.getReceivedMessages();

        assertThat(messages).isEmpty();
    }
}
