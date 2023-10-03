package gr.aegean;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

import gr.aegean.repository.UserRepository;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
public abstract class AbstractIntegrationTest {
    @Autowired
    private UserRepository userRepository;

    /*
        SpringBoot 3.1 and the spring-boot-testcontainers dependency add the @ServiceConnection replaces the following
        code:

        @DynamicPropertySource
        static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }
     */
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
            "postgres:15.2-alpine")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("code_assessment_test");

    static {
        postgreSQLContainer.start();
    }

    @BeforeEach
    void setup() {
        /*
            There is no need to delete password reset tokens and email update tokens because the id of the user is a
            foreign key with ON DELETE CASCADE
         */
        userRepository.deleteAllUsers();
    }
}
