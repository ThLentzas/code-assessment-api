package gr.aegean;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import gr.aegean.repository.EmailUpdateRepository;
import gr.aegean.repository.PasswordResetRepository;
import gr.aegean.repository.UserRepository;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
public abstract class AbstractIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordResetRepository passwordResetRepository;
    @Autowired
    private EmailUpdateRepository emailUpdateRepository;

    static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:15.2-alpine")
                    .withUsername("test")
                    .withPassword("test")
                    .withDatabaseName("code_assessment_test");

    static {
        postgreSQLContainer.start();
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @BeforeEach
    void setup() {
        emailUpdateRepository.deleteAllTokens();
        passwordResetRepository.deleteAllTokens();
        userRepository.deleteAllUsers();
    }
}
