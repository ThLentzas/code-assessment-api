package gr.aegean.integration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import gr.aegean.security.auth.AuthResponse;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles(profiles = "test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private final String AUTH_PATH = "/api/v1/auth";

    @Test
    @Order(1)
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
                .expectStatus()
                .isCreated()
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
                .expectStatus()
                .isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody()
                .getToken();

        assertThat(jwtToken).isNotNull();
    }

    @Test
    @Order(2)
    void shouldNotLoginUserPasswordIsWrong() {
        String requestBody = """
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
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    @Order(3)
    void shouldNotLoginUserWhenEmailIsWrong() {
        String requestBody = """
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
                .expectStatus()
                .isUnauthorized();
    }
}
