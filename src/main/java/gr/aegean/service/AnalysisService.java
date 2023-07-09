package gr.aegean.service;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.analysis.AnalysisRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final ProjectService projectService;
    private final LanguageService languageService;

    public void analyzeProject(AnalysisRequest request) {
        File projectsDirectory = projectService.cloneProject(request);

        try {
            Files.list(projectsDirectory.toPath())
                    .filter(Files::isDirectory)
                    .forEach(folder -> {
                        Map<Double, String> detectedLanguages = languageService.detectLanguage(folder.toString());
                    });

            projectService.deleteProjectsDirectory(projectsDirectory);
        } catch (IOException e) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }
}
