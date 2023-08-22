package gr.aegean.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import gr.aegean.AbstractIntegrationTest;
import gr.aegean.model.user.UserProfile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.*;


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
    void shouldGetUserProfile() {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }""";

        /*
            Returns a byte[] if the response body is empty
         */
        EntityExchangeResult<byte[]> result = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectHeader().exists(HttpHeaders.SET_COOKIE)
                .expectBody()
                .returnResult();

        String cookieHeader = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        /*
            Extracting the token value from the Response Header(SET_COOKIE). The cookie is in the following form:
            accessToken=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjkyNTc2NTk3LCJleHAiOjE2OTI1ODM3OTd9.NOBCnro0mknq0s
            487G-g9QoQ1HGi6TE6FyT2nvKhgfs; Max-Age=3600; Expires=Mon, 21 Aug 2023 01:09:57 GMT; HttpOnly; SameSite=Lax
         */
        String accessToken = cookieHeader.split(";")[0].substring(12);

        UserProfile actual = webTestClient.get()
                .uri(USER_PATH + "/profile")
                .accept(MediaType.APPLICATION_JSON)
                .header(COOKIE, String.format("accessToken=%s", accessToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserProfile.class)
                .returnResult()
                .getResponseBody();

        UserProfile expected = new UserProfile(
                "Test",
                "Test",
                "TestT",
                "I have a real passion for teaching",
                "Cleveland, OH",
                "Code Monkey, LLC"
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldUpdateUserProfile() {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }""";

        /*
            Returns a byte[] if the response body is empty
         */
        EntityExchangeResult<byte[]> result = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectHeader().exists(HttpHeaders.SET_COOKIE)
                .expectBody()
                .returnResult();

        String cookieHeader = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        /*
            Extracting the token value from the Response Header(SET_COOKIE). The cookie is in the following form:
            accessToken=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjkyNTc2NTk3LCJleHAiOjE2OTI1ODM3OTd9.NOBCnro0mknq0s
            487G-g9QoQ1HGi6TE6FyT2nvKhgfs; Max-Age=3600; Expires=Mon, 21 Aug 2023 01:09:57 GMT; HttpOnly; SameSite=Lax.
            The substring(12) will create a String from the 12 character until the length of the String, its 0 based
            index.
         */
        String accessToken = cookieHeader.split(";")[0].substring(12);

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
                .uri(USER_PATH + "/settings/profile")
                .accept(MediaType.APPLICATION_JSON)
                .header(COOKIE, String.format("accessToken=%s", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void shouldUpdateEmail() throws MessagingException {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }""";

        /*
            Returns a byte[] if the response body is empty
         */
        EntityExchangeResult<byte[]> result = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectHeader().exists(HttpHeaders.SET_COOKIE)
                .expectBody()
                .returnResult();

        String cookieHeader = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        /*
            Extracting the token value from the Response Header(SET_COOKIE). The cookie is in the following form:
            accessToken=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjkyNTc2NTk3LCJleHAiOjE2OTI1ODM3OTd9.NOBCnro0mknq0s
            487G-g9QoQ1HGi6TE6FyT2nvKhgfs; Max-Age=3600; Expires=Mon, 21 Aug 2023 01:09:57 GMT; HttpOnly; SameSite=Lax
         */
        String accessToken = cookieHeader.split(";")[0].substring(12);

        requestBody = """
                {
                    "email": "foo@example.com",
                    "password": "CyN549^*o2Cr"
                }
                """;

        webTestClient.post()
                .uri(USER_PATH + "/settings/email")
                .accept(MediaType.APPLICATION_JSON)
                .header(COOKIE, String.format("accessToken=%s", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isAccepted();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        MimeMessage message = messages[0];
        String body = GreenMailUtil.getBody(message);

        // Remove encoded line breaks before extracting URL
        body = body.replace("=\r\n", "");
        Pattern pattern = Pattern.compile("http://localhost:8080/api/v1/users/settings/email[^\"]*");
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
                .uri(USER_PATH + "/settings/email?token={token}", token)
                .header(COOKIE, String.format("accessToken=%s", accessToken))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldDeleteAccount() {
        String requestBody = """
                {
                    "firstname": "Test",
                    "lastname": "Test",
                    "username": "TestT",
                    "email": "test@example.com",
                    "password": "CyN549^*o2Cr",
                    "bio": "I have a real passion for teaching",
                    "location": "Cleveland, OH",
                    "company": "Code Monkey, LLC"
                }""";

        /*
            Returns a byte[] if the response body is empty
         */
        EntityExchangeResult<byte[]> result = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectHeader().exists(HttpHeaders.SET_COOKIE)
                .expectBody()
                .returnResult();

        String cookieHeader = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        /*
            Extracting the token value from the Response Header(SET_COOKIE). The cookie is in the following form:
            accessToken=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjkyNTc2NTk3LCJleHAiOjE2OTI1ODM3OTd9.NOBCnro0mknq0s
            487G-g9QoQ1HGi6TE6FyT2nvKhgfs; Max-Age=3600; Expires=Mon, 21 Aug 2023 01:09:57 GMT; HttpOnly; SameSite=Lax
         */
        String accessToken = cookieHeader.split(";")[0].substring(12);

        webTestClient.delete()
                .uri(USER_PATH + "/settings/account")
                .accept(MediaType.APPLICATION_JSON)
                .header(COOKIE, String.format("accessToken=%s", accessToken))
                .exchange()
                .expectStatus().isNoContent();
    }
}