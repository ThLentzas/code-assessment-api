package gr.aegean.service;

import gr.aegean.exception.ServerErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.TreeMap;


@Service
@RequiredArgsConstructor
public class LanguageService {
    private final ProcessBuilder processBuilder;

    public TreeMap<Double, String> detectLanguage(String path)  {
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

                String [] linguistOutput = reportBuilder.toString().split(" ");
                Double percentage = Double.parseDouble(linguistOutput[0].replace("%", ""));
                String language = linguistOutput[linguistOutput.length - 1];

                languages.put(percentage, language);

                reportBuilder = new StringBuilder();
            }
        } catch (IOException ioe) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        return languages;
    }
}
