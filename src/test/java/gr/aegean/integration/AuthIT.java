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

import gr.aegean.AbstractIntegrationTest;
import gr.aegean.model.dto.auth.AuthResponse;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


/*
    We test the happy paths in ITs.
 */
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

        assertThat(response.token()).isNotNull();

        requestBody = """
                {
                    "email": "test@example.com",
                    "password": "Igw4UQAlfX$E"
                }
                """;

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
    void shouldResetPassword() throws MessagingException {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "Test",
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

        assertThat(response.token()).isNotNull();

        requestBody = """
                {
                    "email": "test@example.com"
                }
                """;

        webTestClient.post()
                .uri(AUTH_PATH + "/password_reset")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isAccepted();

        /*
          Extracting the token from the send email to be used in the GET request. The url part is encoded since we
          can't click it so our browser can do the decoding we have to extract the token from the encoded url for
          testing only. The decoding process is down by our browser automatically when we click the email link.

          Sending the email is done async, so we have to use awaitility.
         */
        await().atMost(5, TimeUnit.SECONDS).until(() -> greenMail.getReceivedMessages().length == 1);

        MimeMessage[] messages = greenMail.getReceivedMessages();
        MimeMessage message = messages[0];
        String body = GreenMailUtil.getBody(message);

        // Remove encoded line breaks before extracting the reset link
        body = body.replace("=\r\n", "");
        Pattern pattern = Pattern.compile("http://localhost:4200/password_reset/confirm[^\"]*");
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

        requestBody = String.format("""
                {
                    "token": "%s",
                    "password": "3frMH4v!20d4"
                }
                """, token);

        webTestClient.put()
                .uri(AUTH_PATH + "/password_reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();

        /*
            Sending the email is done async, so we have to use awaitility. The length of the received messages is 2
            because we already sent the password reset link email, and now we sent the confirmation one.
         */
        await().atMost(5, TimeUnit.SECONDS).until(() -> greenMail.getReceivedMessages().length == 2);

        messages = greenMail.getReceivedMessages();
        message = messages[1];

        //Assertions for the password reset confirmation email
        assertThat(message.getAllRecipients()).hasSize(1);
        assertThat(message.getAllRecipients()[0]).hasToString("test@example.com");
        assertThat(message.getSubject()).isEqualTo("Your password was reset");

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
}
