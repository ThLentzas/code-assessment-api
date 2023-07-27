package gr.aegean.service.analysis;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.analysis.Language;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class LanguageService {

    public Map<String, Double> detectLanguage(String path) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "docker",
                "run",
                "--rm",
                "-v",
                path + ":/code",
                "linguist",
                "github-linguist",
                "/code"
        );

        Map<String, Double> languages = new HashMap<>();
        StringBuilder reportBuilder = new StringBuilder();

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                reportBuilder.append(line);

                String[] linguistOutput = reportBuilder.toString().split(" ");
                String language = linguistOutput[linguistOutput.length - 1];
                Double percentage = Double.parseDouble(linguistOutput[0].replace("%", ""));

                languages.put(language, percentage);
                reportBuilder = new StringBuilder();
            }
        } catch (IOException ioe) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        return languages;
    }

    public boolean verifySupportedLanguages(Map<String, Double> detectedLanguages) {
        Set<String> supportedLanguages = Arrays.stream(Language.values())
                .map(Language::name)
                .collect(Collectors.toSet());

        return detectedLanguages.keySet().stream()
                .map(String::toUpperCase)
                .anyMatch(supportedLanguages::contains);
    }
}
