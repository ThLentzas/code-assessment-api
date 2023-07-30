package gr.aegean.service.analysis;

import gr.aegean.entity.Analysis;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.mapper.dto.AnalysisReportDTOMapper;
import gr.aegean.model.analysis.AnalysisReportDTO;
import gr.aegean.model.analysis.AnalysisRequest;
import gr.aegean.model.analysis.AnalysisResult;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.repository.AnalysisRepository;
import gr.aegean.service.assessment.AssessmentService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
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
    private final AssessmentService assessmentService;
    @Value("${sonar.token}")
    private String authToken;
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";


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
            EnumMap<QualityMetric, Double> updatedQualityMetricsReport = metricCalculationService.applyUtf(
                    analysisReport.getQualityMetricsReport(),
                    analysisReport.getIssuesReport().getIssues(),
                    analysisReport.getHotspotsReport().getHotspots());

            analysisReport.setQualityMetricsReport(updatedQualityMetricsReport);
            analysisReport.setProjectUrl(Link.of(projectUrl));
        } catch (IOException | InterruptedException ioe) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
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
            throw new ServerErrorException(SERVER_ERROR_MSG);
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

    public Integer saveAnalysisProcess(Integer userId, List<AnalysisReport> reports, AnalysisRequest analysisRequest) {
        Integer analysisId = saveAnalysis(userId);
        saveAllAnalysisReports(analysisId, reports);
        saveConstraint(analysisId, analysisRequest.constraints());
        savePreference(analysisId, analysisRequest.preferences());

        return analysisId;
    }

    public AnalysisResult findAnalysisResultByAnalysisId(Integer analysisId) {
        Analysis analysis = analysisRepository.findAnalysisByAnalysisId(analysisId).orElseThrow(() ->
                new ResourceNotFoundException("No analysis was found for analysis id: "+ analysisId));
        List<AnalysisReport> reports = analysisRepository.findAnalysisReportsByAnalysisId(analysisId).orElseThrow(() ->
                new ResourceNotFoundException("Analysis reports were not found for analysis with id: " + analysisId));

        /*
            If no constraints or/and no preferences were found meaning no constraints or/and no preferences were
             provided for the specific analysis, we have an empty list.
         */
        List<Constraint> constraints = analysisRepository.findAnalysisConstraintsByAnalysisId(analysisId).orElse(
                Collections.emptyList());
        List<Preference> preferences = analysisRepository.findAnalysisPreferencesByAnalysisId(analysisId).orElse(
                Collections.emptyList());

        List<List<AnalysisReport>> rankedReports = assessmentService.assessAnalysisResult(
                reports, constraints, preferences);

        List<List<AnalysisReportDTO>> rankedReportsDTO = rankedReports.stream()
                .map(list -> list.stream()
                        .map(mapper)
                        .toList())
                .toList();

        return new AnalysisResult(rankedReportsDTO, analysis.getCreatedDate());
    }

    public AnalysisReportDTO findAnalysisReportById(Integer reportId) {
        AnalysisReport report = analysisRepository.findAnalysisReportByReportId(reportId).orElseThrow(() ->
                new ResourceNotFoundException("Analysis report was not found for analysis with id" + reportId));

        return mapper.apply(report);
    }

    public List<Analysis> findAnalysesByUserId(Integer userId) {
        return analysisRepository.findAnalysesByUserId(userId).orElse(Collections.emptyList());
    }

    private int saveAnalysis(Integer userId) {
        return analysisRepository.saveAnalysis(new Analysis(userId, LocalDateTime.now()));
    }

    private void saveAllAnalysisReports(Integer analysisId, List<AnalysisReport> reports) {
        reports.forEach(report -> {
            report.setAnalysisId(analysisId);
            analysisRepository.saveAnalysisReport(report);
        });
    }

    private void saveConstraint(Integer analysisId, List<Constraint> constraints) {
        if (constraints != null && !constraints.isEmpty()) {
            constraints.forEach(constraint -> {
                constraint.setAnalysisId(analysisId);
                analysisRepository.saveAnalysisConstraint(constraint);
            });
        }
    }

    private void savePreference(Integer analysisId, List<Preference> preferences) {
        if (preferences != null && !preferences.isEmpty()) {
            preferences.forEach(preference -> {
                preference.setAnalysisId(analysisId);
                analysisRepository.saveAnalysisPreference(preference);
            });
        }
    }
}

