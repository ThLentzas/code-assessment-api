package gr.aegean.service;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.entity.AnalysisReport;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;



@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final LanguageService languageService;
    private final SonarService sonarService;
    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);

    public Optional<AnalysisReport> analyzeProjects(Path project) {
        logger.info("Analyzing project at path: " + project.toString());
        AnalysisReport analysisReport = null;
        System.out.println(Thread.currentThread().getName());

        /*
            For each project we downloaded and stored locally we detect the languages used.
         */
        Map<Double, String> detectedLanguages = languageService.detectLanguage(project.toString());
        System.out.println(detectedLanguages);
        if (!languageService.verifySupportedLanguages(detectedLanguages)) {
            //toDO: What should we do when a language that is supported gets detected.

            throw new IllegalArgumentException("Please submit repositories based on the supported languages");
        }

        if (detectedLanguages.containsValue("Java")) {
            /*
                Find the path of pom.xml and src.
            */
            findFilePath(project.toString(), "pom.xml")
                    .ifPresent(pomXmlPath -> findFilePath(project.toString(), "src")
                            .ifPresent(srcPath ->
                                    analyzeMavenProject(project.toString(), srcPath, pomXmlPath)));
        } else {
            String projectKey = project.toString().split("\\\\")[3];
            sonarService.analyzeProject(projectKey, project.toString());

            try {
                analysisReport = sonarService.createAnalysisReport(projectKey);

            } catch (IOException | InterruptedException ioe) {
                ioe.printStackTrace();
                throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                        "request. Please try again later.");
            }
        }

        return Optional.ofNullable(analysisReport);
    }

    private Optional<String> findFilePath(String projectDirectory, String requestedFile) {
        Path path = Paths.get(projectDirectory);

         /*
            Recursively checking for the directory of a pom.xml file or the src. Only try with resources can be used
            without catch or finally. Lambda expressions in Java cannot propagate checked exceptions without extra
            handling. That's why we need to use try/catch here despite being called inside a try catch block.
         */
        try (Stream<Path> paths = Files.walk(path)) {
            return paths
                    .filter(p -> p.getFileName().toString().equals(requestedFile))
                    .findFirst()
                    .map(Path::toString);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }

    private void analyzeMavenProject(String directoryPath, String srcPath, String pomXmlPath) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        Path dockerfilePath = Paths.get(directoryPath, "Dockerfile");
        Path directory = Paths.get(directoryPath);
        Path src = Paths.get(srcPath);
        Path pomXml = Paths.get(pomXmlPath);
        Path srcRelative = directory.relativize(src);
        Path pomXmlRelative = directory.relativize(pomXml);

        String dockerfileContent = String.format("""              
                {
                    FROM maven:3.6.3-jdk-11
                    WORKDIR /app
                    COPY %s .
                    COPY %s ./src
                    CMD ["mvn", "clean"]
                }
                """, pomXmlRelative, srcRelative);

        try {
            /*
                1st argument: the path to write the docker file. The root directory of the project
                2nd argument: the content to write in the file.
                If there is a dockerfile named "Dockerfile" it will be overwritten by ours.
             */
            Files.write(dockerfilePath, dockerfileContent.getBytes());

            /*
                Splitting with the escape character which is also the file seperator in Windows
             */
            String dockerImage = directoryPath.split("\\\\")[3];
            processBuilder.command(
                    "docker",
                    "build",
                    "-t",
                    dockerImage,
                    "."
            );

            /*
                Setting the directory of the command execution to be the projects directory, so we can use .
             */
            processBuilder.directory(new File(directoryPath));
            processBuilder.start();
            processBuilder.wait();
        } catch (IOException | InterruptedException ie) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }
}

