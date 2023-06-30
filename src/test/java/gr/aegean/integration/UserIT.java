package gr.aegean.integration;

import gr.aegean.AbstractIntegrationTest;
import gr.aegean.model.auth.AuthResponse;
import gr.aegean.model.user.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
class UserIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private static final String USER_PATH = "/api/v1/users";
    private final String AUTH_PATH = "/api/v1/auth";

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

        EntityExchangeResult<AuthResponse> result = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(AuthResponse.class)
                .returnResult();

        String jwtToken = result.getResponseBody().getToken();
        String locationHeader = result.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        Integer userId = Integer.parseInt(locationHeader.substring(locationHeader.lastIndexOf('/') + 1));

        UserProfile actual = webTestClient.get()
                .uri(USER_PATH + "/{userId}/profile", userId)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
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

        EntityExchangeResult<AuthResponse> result = webTestClient.post()
                .uri(AUTH_PATH + "/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(AuthResponse.class)
                .returnResult();

        String jwtToken = result.getResponseBody().getToken();
        String locationHeader = result.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        Integer userId = Integer.parseInt(locationHeader.substring(locationHeader.lastIndexOf('/') + 1));

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
                .uri(USER_PATH + "/{userId}/settings/profile", userId)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, String.format("Bearer %s", jwtToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();
    }
}