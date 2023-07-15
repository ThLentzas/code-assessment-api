package gr.aegean.service;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.analysis.language.Language;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import java.util.stream.Collectors;


@Service
public class LanguageService {
    private static final Logger logger = LoggerFactory.getLogger(LanguageService.class);

    public TreeMap<Double, String> detectLanguage(String path) {
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

        logger.info("Starting Docker process with command: " + Arrays.toString(processBuilder.command().toArray()));

        TreeMap<Double, String> languages = new TreeMap<>((Collections.reverseOrder()));
        StringBuilder reportBuilder = new StringBuilder();

        try {
            logger.info("Docker process started");
            Process process = processBuilder.start();
            logger.info("Docker process finished");

            logger.info("Starting to read Docker process output stream");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                logger.info("Docker process output: " + line);
                reportBuilder.append(line);

                String[] linguistOutput = reportBuilder.toString().split(" ");
                Double percentage = Double.parseDouble(linguistOutput[0].replace("%", ""));
                String language = linguistOutput[linguistOutput.length - 1];

                languages.put(percentage, language);
                reportBuilder = new StringBuilder();
            }
            logger.info("Finished reading Docker process output stream");
        } catch (IOException ioe) {
            logger.error("Error while executing Docker command or reading its output", ioe);
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your request. Please try again later.");
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
