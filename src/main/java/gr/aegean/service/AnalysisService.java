package gr.aegean.service;

import gr.aegean.entity.Analysis;
import gr.aegean.entity.QualityMetricDetails;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.repository.AnalysisRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final LanguageService languageService;
    private final SonarService sonarService;
    private final AnalysisRepository analysisRepository;

    public Optional<AnalysisReport> analyzeProjects(Path project) {
        AnalysisReport analysisReport = null;

        /*
            For each project we downloaded and stored locally we detect the languages used.
         */
        Map<String, Double> detectedLanguages = languageService.detectLanguage(project.toString());
        if (!languageService.verifySupportedLanguages(detectedLanguages)) {
            return Optional.empty();
        }

        if (detectedLanguages.containsKey("Java")) {

            /*
                Find the path of pom.xml and src.
            */
            if (isMavenProject(project)) {
                analyzeMavenProject(project.toString());

            } else if (isGradleProject(project)) {

            }
        } else {
            String projectKey = project.toString().split("\\\\")[3];
            sonarService.analyzeProject(projectKey, project.toString());

            /*
                We need the userId to store the analysis alongside with the related user.
             */

            try {
                analysisReport = sonarService.createAnalysisReport(projectKey, detectedLanguages);

            } catch (IOException | InterruptedException ioe) {
                throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                        "request. Please try again later.");
            }
        }

        return Optional.ofNullable(analysisReport);
    }

    public boolean isMavenProject(Path project) {
        try (Stream<Path> paths = Files.walk(project)) {
            return paths.anyMatch(p -> p.getFileName().toString().equals("pom.xml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isGradleProject(Path project) {
        try (Stream<Path> paths = Files.walk(project)) {
            return paths.anyMatch(p -> p.getFileName().toString().equals("build.gradle"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
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

    private void analyzeMavenProject(String projectPath) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        Path dockerfilePath = Paths.get(projectPath, "Dockerfile");
        Map<String, String> versions = fetchVersionsFromPom(projectPath);
        String mavenVersion = versions.get("Maven");
        String javaVersion = versions.get("Java");

        String dockerfileContent = String.format("""              
                {
                    FROM maven:%s-openjdk-%s
                    WORKDIR /app
                    COPY . .
                    CMD ["mvn", "clean"]
                }
                """, mavenVersion, javaVersion);

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
            String dockerImage = projectPath.split("\\\\")[3];
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
            processBuilder.directory(new File(projectPath));
            processBuilder.start();
            processBuilder.wait();
        } catch (IOException | InterruptedException ie) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }

    public Map<String, String> fetchVersionsFromPom(String projectPath) {
        Map<String, String> versions = new HashMap<>();

        try {
            // Append "pom.xml" to the project path
            String pomPath = Paths.get(projectPath, "pom.xml").toString();

            // Open the pom.xml file
            File inputFile = new File(pomPath);
            if (!inputFile.exists() || inputFile.isDirectory()) {
                System.err.println("pom.xml not found at the provided project path.");
                return versions;
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Create XPath object
            XPath xPath = XPathFactory.newInstance().newXPath();

            // Get Java version
            Node javaVersionNode = (Node) xPath.compile("/project/properties/java.version")
                    .evaluate(doc, XPathConstants.NODE);
            if (javaVersionNode != null) {
                versions.put("Java", javaVersionNode.getTextContent());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(versions);

        return versions;
    }

    public Analysis saveAnalysis(Analysis analysis) {
        return analysisRepository.saveAnalysis(analysis);
    }

    public void saveAnalysisReport(AnalysisReport analysisReport) {
        analysisRepository.saveAnalysisReport(analysisReport);
    }

    public void saveQualityMetricDetails(Integer analysisId, List<QualityMetricDetails> metricDetails) {
        metricDetails.forEach(details -> analysisRepository.saveQualityMetricDetails(analysisId, details));
    }
}

