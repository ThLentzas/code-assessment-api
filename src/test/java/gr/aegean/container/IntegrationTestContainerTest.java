package gr.aegean.container;

import gr.aegean.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class IntegrationTestContainerTest extends AbstractIntegrationTest {

    @Test
    void connectionEstablished() {
        assertThat(postgreSQLContainer.isRunning()).isTrue();
        assertThat(postgreSQLContainer.isCreated()).isTrue();
    }
}
