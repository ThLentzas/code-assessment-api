package gr.aegean.service;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.analysis.language.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class LanguageService {
    private final ProcessBuilder processBuilder;

    public TreeMap<Double, String> detectLanguage(String path) {
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

        /*
            The default order is ascending, but we need to have the language with the highest percentage first.
         */
        TreeMap<Double, String> languages = new TreeMap<>((Collections.reverseOrder()));
        StringBuilder reportBuilder = new StringBuilder();

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                reportBuilder.append(line);

                String[] linguistOutput = reportBuilder.toString().split(" ");
                Double percentage = Double.parseDouble(linguistOutput[0].replace("%", ""));
                String language = linguistOutput[linguistOutput.length - 1];

                /*
                    toDO: Make sure the language added is supported basically if it is a value from the enum Language.
                     If not return an empty map and in analysis service if the language map is empty return an empty
                     analysis report.
                 */
                languages.put(percentage, language);

                reportBuilder = new StringBuilder();
            }
        } catch (IOException ioe) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        return languages;
    }

    public boolean verifySupportedLanguages(Map<Double, String> detectedLanguages) {
        Set<String> supportedLanguages = Arrays.stream(Language.values())
                .map(Language::name)
                .collect(Collectors.toSet());

        return detectedLanguages.values().stream()
                .map(String::toUpperCase)
                .anyMatch(supportedLanguages::contains);
    }
}
