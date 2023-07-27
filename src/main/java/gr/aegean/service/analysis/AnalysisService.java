package gr.aegean.service.analysis;

import gr.aegean.entity.Analysis;
import gr.aegean.entity.QualityMetricDetails;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.mapper.AnalysisReportDTOMapper;
import gr.aegean.model.analysis.AnalysisReportDTO;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.repository.AnalysisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final LanguageService languageService;
    private final SonarService sonarService;
    private final MetricCalculationService metricCalculationService;
    private final AnalysisRepository analysisRepository;
    private final AnalysisReportDTOMapper mapper;
    @Value("${sonar.token}")
    private String authToken;

    public Optional<AnalysisReport> analyzeProject(Path projectPath, String projectUrl) {
        AnalysisReport analysisReport;

        /*
            For each project we downloaded and stored locally we detect the languages used.
         */
        Map<String, Double> detectedLanguages = languageService.detectLanguage(projectPath.toString());
        if (!languageService.verifySupportedLanguages(detectedLanguages)) {
            return Optional.empty();
        }

        String projectKey = projectPath.toString().split("\\\\")[3];

        try {
            if (detectedLanguages.containsKey("Java")) {
                if (isMavenProject(projectPath)) {
                    analyzeMavenProject(projectKey, projectPath.toString());
                }
            } else {
                sonarService.analyzeProject(projectKey, projectPath.toString());
            }

            analysisReport = sonarService.fetchAnalysisReport(projectKey, detectedLanguages);
            EnumMap<QualityMetric, Double> updatedQualityMetricReport = metricCalculationService.applyUtf(
                    analysisReport.getQualityMetricReport(),
                    analysisReport.getIssuesReport().getIssues());

            analysisReport.setQualityMetricReport(updatedQualityMetricReport);
            analysisReport.setProjectUrl(Link.of(projectUrl));
        } catch (IOException | InterruptedException ioe) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        return Optional.of(analysisReport);
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

    private void analyzeMavenProject(String projectKey, String projectPath) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        String dockerImage;

        try {
            createDockerFile(projectKey, projectPath);
            dockerImage = buildDockerImage(projectPath, processBuilder);
            runDockerContainer(dockerImage, projectPath, processBuilder);
        } catch (IOException | InterruptedException ie) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }

    private void createDockerFile(String projectKey, String projectPath) throws IOException {
        Path dockerfilePath = Paths.get(projectPath, "Dockerfile");
        String dockerfileContent = String.format("""
                    FROM maven:3.8.7-openjdk-18-slim
                    WORKDIR /app
                    COPY . .
                    CMD sh -c 'mvn clean verify sonar:sonar \
                    -Dmaven.test.skip=true \
                    -Dsonar.host.url=http://sonarqube:9000 \
                    -Dsonar.projectKey=%s \
                    -Dsonar.login=%s;'
                """, projectKey, authToken);

        /*
            1st argument: the path to write the docker file. The root directory of the project.
            2nd argument: the content to write in the file.
            If there is a dockerfile named "Dockerfile" it will be overwritten by ours.
         */
        Files.write(dockerfilePath, dockerfileContent.getBytes());
    }

    private String buildDockerImage(String projectPath, ProcessBuilder processBuilder) throws
            IOException, InterruptedException {
        /*
            Splitting with the escape character which is also the file separator in Windows
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
        Process process = processBuilder.start();
        process.waitFor();

        return dockerImage;
    }

    private void runDockerContainer(String dockerImage, String projectPath, ProcessBuilder processBuilder) throws
            InterruptedException, IOException {
        String containerName = projectPath.split("\\\\")[3];

        processBuilder.command(
                "docker",
                "run",
                "--rm",
                "--name",
                containerName,
                "--network",
                "code-assessment-net",
                dockerImage
        );
        processBuilder.start();
    }

    public int saveAnalysis(Analysis analysis) {
       return  analysisRepository.saveAnalysis(analysis);
    }

    public void saveAnalysisReport(AnalysisReport analysisReport) {
        analysisRepository.saveAnalysisReport(analysisReport);
    }

    public void saveQualityMetricDetails(Integer analysisId, List<QualityMetricDetails> metricDetails) {
        metricDetails.forEach(details -> {
                details.setAnalysisId(analysisId);
                analysisRepository.saveQualityMetricDetails(details);
        });
    }

    public List<AnalysisReportDTO> findAnalysisReportByAnalysisId(Integer analysisId) {
        List<AnalysisReport> reports = analysisRepository.findAnalysisReportByAnalysisId(analysisId).orElseThrow(() ->
                new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                        "request. Please try again later."));

        return reports.stream()
                .map(mapper)
                .toList();
    }

    public AnalysisReportDTO findAnalysisReportById(Integer analysisId) {
        AnalysisReport report = analysisRepository.findAnalysisReportById(analysisId).orElseThrow(() ->
                new ServerErrorException("The server encountered an internal error and was unable " + "to complete " +
                        "your request. Please try again later."));

        return mapper.apply(report);
    }
}

