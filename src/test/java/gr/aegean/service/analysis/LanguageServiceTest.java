package gr.aegean.service.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class LanguageServiceTest {
    @Mock
    private DockerService dockerService;
    private LanguageService underTest;


    @BeforeEach
    void setup() {
        underTest = new LanguageService(dockerService);
    }

    @Test
    void shouldDetectLanguagesCorrectly() {
        when(dockerService.createLinguistContainer(any(String.class))).thenReturn("100.00% 31261      Python");

        Map<String, Double> actual = underTest.detectLanguage("path");

        assertThat(actual).hasSize(1)
                .containsKey("Python")
                .containsEntry("Python", 100.0);
    }

    @Test
    void shouldReturnTrueWhenAtLeastOneDetectedLanguageIsSupported() {
        Map<String, Double> detectedLanguages = new HashMap<>();

        /*
            Go is supported, C++ is not
         */
        detectedLanguages.put("Go", 25.0);
        detectedLanguages.put("C++", 31.3);

        boolean actual = underTest.verifySupportedLanguages(detectedLanguages);

        assertThat(actual).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNoneOfTheDetectedLanguagesAreSupported() {
        Map<String, Double> detectedLanguages = new HashMap<>();

        /*
            Neither Pascal nor C++ are supported
         */
        detectedLanguages.put("Pascal", 25.0);
        detectedLanguages.put("C++", 31.3);

        boolean actual = underTest.verifySupportedLanguages(detectedLanguages);

        assertThat(actual).isFalse();
    }
}
