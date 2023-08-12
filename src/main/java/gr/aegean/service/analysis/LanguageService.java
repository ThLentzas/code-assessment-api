package gr.aegean.service.analysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import gr.aegean.model.analysis.Language;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class LanguageService {
    private final DockerService dockerService;

    public Map<String, Double> detectLanguage(String path) {
        String containerOutput = dockerService.createLinguistContainer(path);

        return parseLinguistOutput(containerOutput);
    }

    /*
       The output of GitHub linguist is in the form of: 100.00% 31261      Python
     */
    private Map<String, Double> parseLinguistOutput(String containerOutput) {
        Map<String, Double> languages = new HashMap<>();
        String[] lines = containerOutput.split("\n");

        for (String line : lines) {
            String[] linguistOutput = line.split(" ");
            String language = linguistOutput[linguistOutput.length - 1];
            Double percentage = Double.parseDouble(linguistOutput[0].replace("%", ""));

            languages.put(language, percentage);
        }
        return languages;
    }

    /**
     * @return True if any detected language matches the supported ones, false otherwise.
     */
    public boolean verifySupportedLanguages(Map<String, Double> detectedLanguages) {
        Set<String> supportedLanguages = Arrays.stream(Language.values())
                .map(Language::name)
                .collect(Collectors.toSet());

        return detectedLanguages.keySet().stream()
                .map(String::toUpperCase)
                .anyMatch(supportedLanguages::contains);
    }
}