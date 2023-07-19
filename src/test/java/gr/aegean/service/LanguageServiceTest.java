package gr.aegean.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageServiceTest {
    private LanguageService underTest;

    @BeforeEach
    void setup() {
        underTest = new LanguageService();
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
