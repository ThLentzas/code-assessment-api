package gr.aegean.service.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;


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
    void shouldDetectLanguages() {
        //Arrange
        when(dockerService.createLinguistContainer(any(String.class))).thenReturn("100.00% 31261      Python");

        //Act
        Map<String, Double> actual = underTest.detectLanguages("path");

        //Assert
        assertThat(actual)
                .hasSize(1)
                .containsEntry("Python", 100.0);
    }

    @Test
    void shouldReturnTrueWhenAtLeastOneDetectedLanguageIsSupported() {
        //Arrange
        Map<String, Double> detectedLanguages = new HashMap<>();

        /*
            Go is supported, C++ is not
         */
        detectedLanguages.put("Go", 25.0);
        detectedLanguages.put("C++", 31.3);

        //Act
        boolean actual = underTest.verifySupportedLanguages(detectedLanguages);

        //Assert
        assertThat(actual).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNoneOfTheDetectedLanguagesAreSupported() {
        //Arrange
        Map<String, Double> detectedLanguages = new HashMap<>();

        /*
            Neither Pascal nor C++ are supported
         */
        detectedLanguages.put("Pascal", 25.0);
        detectedLanguages.put("C++", 31.3);

        //Act
        boolean actual = underTest.verifySupportedLanguages(detectedLanguages);

        //Assert
        assertThat(actual).isFalse();
    }
}
