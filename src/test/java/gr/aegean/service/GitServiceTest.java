package gr.aegean.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

class GitServiceTest {
    private GitService underTest;

    @BeforeEach
    void setup() {
        underTest = new GitService();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://gitlab.com/user/repo", // different host
            "https://github.com@user/repo",
            "https://github.com.malicious.com"//malicious url
    })
    void shouldReturnFalseForInvalidHostInTheURI(String url) {
        boolean isValid = underTest.isValidGitHubUrl(url);

        assertThat(isValid).isFalse();
    }
}
