package gr.aegean.service.analysis;

import gr.aegean.entity.Analysis;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.mapper.dto.AnalysisReportDTOMapper;
import gr.aegean.model.analysis.quality.QualityAttribute;
import gr.aegean.model.analysis.quality.TreeNode;
import gr.aegean.model.analysis.sonarqube.HotspotsReport;
import gr.aegean.model.analysis.sonarqube.IssuesReport;
import gr.aegean.model.dto.analysis.AnalysisReportDTO;
import gr.aegean.model.dto.analysis.AnalysisRequest;
import gr.aegean.model.dto.analysis.AnalysisResponse;
import gr.aegean.model.dto.analysis.RefreshRequest;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.repository.AnalysisRepository;
import gr.aegean.service.assessment.AssessmentService;
import gr.aegean.service.assessment.TreeService;
import gr.aegean.service.auth.JwtService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.hateoas.Link;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final LanguageService languageService;
    private final SonarService sonarService;
    private final MetricService metricService;
    private final AssessmentService assessmentService;
    private final DockerService dockerService;
    private final JwtService jwtService;
    private final TreeService treeService;
    private final AnalysisRepository analysisRepository;
    private final AnalysisReportDTOMapper mapper = new AnalysisReportDTOMapper();
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";


    public Optional<AnalysisReport> analyze(Path projectPath, String projectUrl) {
        AnalysisReport analysisReport;

        /*
            For each project we downloaded and stored locally we detect the languages used.
         */
        Map<String, Double> detectedLanguages = languageService.detectLanguage(projectPath.toString());
        if (!languageService.verifySupportedLanguages(detectedLanguages)) {
            return Optional.empty();
        }

        /*
            The project key is the UUID that was assigned to the project folder. Splitting with the escape character
            which is also the file separator in Windows.
         */
        String projectKey = projectPath.toString().split("\\\\")[3];

        if (detectedLanguages.containsKey("Java")) {
            if (isMavenProject(projectPath)) {
                analyzeMavenProject(projectKey, projectPath.toString());
            } else {
                return Optional.empty();
            }
        } else {
            sonarService.analyzeProject(projectKey, projectPath.toString());
        }

        analysisReport = sonarService.fetchAnalysisReport(projectKey);
        processAnalysisReport(analysisReport, detectedLanguages, projectUrl);

        return Optional.of(analysisReport);
    }

    public Integer saveAnalysisProcess(Integer userId,
                                       List<AnalysisReport> reports,
                                       List<Constraint> constraints,
                                       List<Preference> preferences) {
        Integer analysisId = saveAnalysis(userId);
        saveAllAnalysisReports(analysisId, reports);
        saveConstraints(analysisId, constraints);
        savePreferences(analysisId, preferences);

        return analysisId;
    }

    public AnalysisResponse findAnalysisResultByAnalysisId(Integer analysisId) {
        Analysis analysis = findAnalysisByAnalysisId(analysisId);
        List<AnalysisReport> reports = findAnalysisReportsByAnalysisId(analysisId);

        /*
            If no constraints or/and no preferences were found meaning no constraints or/and no preferences were
            provided for the specific analysis, we have an empty list.
         */
        List<Constraint> constraints = findAnalysisConstraintsByAnalysisId(analysisId);
        List<Preference> preferences = findAnalysisPreferencesByAnalysisId(analysisId);

        List<List<AnalysisReport>> rankedReports = assessmentService.assessAnalysisResult(
                reports, constraints, preferences);

        List<List<AnalysisReportDTO>> rankedReportsDTO = rankedReports.stream()
                .map(list -> list.stream()
                        .map(mapper)
                        .toList())
                .toList();

        return new AnalysisResponse(analysis.getId(), rankedReportsDTO, analysis.getCreatedDate());
    }

    public AnalysisResponse refreshAnalysisResult(Integer analysisId, RefreshRequest request) {
        validateRefreshRequest(request);

        Analysis analysis = findAnalysisByAnalysisId(analysisId);
        List<AnalysisReport> reports = findAnalysisReportsByAnalysisId(analysisId);

        /*
            For null values an empty list will be assigned.
         */
        List<Constraint> constraints = request.constraints();
        List<Preference> preferences = request.preferences();
        List<List<AnalysisReport>> rankedReports = assessmentService.assessAnalysisResult(
                reports, constraints, preferences);

        /*
            Updating wouldn't work, also DELETE ON CASCADE wouldn't work either because it would actually delete the
            initial analysis, so we delete first the old constraints/preferences and then save the new ones.
         */
        analysisRepository.deleteConstraintByAnalysisId(analysisId);
        analysisRepository.deletePreferenceByAnalysisId(analysisId);
        saveConstraints(analysisId, constraints);
        savePreferences(analysisId, preferences);

        List<List<AnalysisReportDTO>> rankedReportsDTO = rankedReports.stream()
                .map(list -> list.stream()
                        .map(mapper)
                        .toList())
                .toList();

        return new AnalysisResponse(analysis.getId(), rankedReportsDTO, analysis.getCreatedDate());
    }

    /**
     * @return The entire user's history. Will return an empty list if no history is found.
     */
    public List<Analysis> getHistory(Integer userId) {
        return analysisRepository.getHistory(userId).orElse(Collections.emptyList());
    }

    /**
     * @return The user's history in a given date range. Will return an empty list if no history is found.
     */
    public List<Analysis> getHistoryInDateRange(Integer userId, Date from, Date to) {
        return analysisRepository.getHistoryInDateRange(userId, from, to).orElse(Collections.emptyList());
    }

    public AnalysisRequest findAnalysisRequestByAnalysisId(Integer analysisId) {
        List<AnalysisReport> reports = findAnalysisReportsByAnalysisId(analysisId);
        List<Constraint> constraints = findAnalysisConstraintsByAnalysisId(analysisId);
        List<Preference> preferences = findAnalysisPreferencesByAnalysisId(analysisId);

        List<String> projectUrls = reports.stream()
                .map(report -> report.getProjectUrl().getHref())
                .toList();

        return new AnalysisRequest(projectUrls, constraints, preferences);
    }

    public void deleteAnalysis(Integer analysisId) {
        int userId = Integer.parseInt(jwtService.getSubject());

        analysisRepository.deleteAnalysis(analysisId, userId);
    }

    /*
        We don't have to check if we have invalid quality metric values, it will be handled by the deserializer during
        the deserialization.
     */
    public void validateConstraints(List<Constraint> constraints) {
        if (constraints == null || constraints.isEmpty()) {
            return;
        }

        Set<QualityMetric> qualityMetrics = constraints.stream()
                .map(Constraint::getQualityMetric)
                .collect(Collectors.toSet());

        /*
            Case: Duplicate quality metric was provided.
         */
        if (qualityMetrics.size() != constraints.size()) {
            throw new IllegalArgumentException("Invalid constraint values. Avoid duplicates");
        }

        /*
            All threshold values must be in the range of [0.0 - 1.0]
         */
        boolean isValidThreshold = constraints.stream()
                .allMatch(constraint -> constraint.getThreshold() <= 1.0 && constraint.getThreshold() >= 0);
        if (!isValidThreshold) {
            throw new IllegalArgumentException("Threshold values must be in the range of [0.0 - 1.0]");
        }
    }

    /*
        We don't have to check if we have invalid quality attribute values, it will be handled by the deserializer
        during the deserialization.
     */
    public void validatePreferences(List<Preference> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return;
        }

        Set<QualityAttribute> qualityAttributes = preferences.stream()
                .map(Preference::getQualityAttribute)
                .collect(Collectors.toSet());

        /*
            Case: Duplicate quality attribute was provided.
         */
        if (qualityAttributes.size() != preferences.size()) {
            throw new IllegalArgumentException("Invalid preference values. Avoid duplicates");
        }

        /*
            All weight values must be in the range of [0.0 - 1.0]
         */
        boolean isValidWeight = preferences.stream()
                .allMatch(preference -> preference.getWeight() <= 1.0 && preference.getWeight() >= 0);
        if (!isValidWeight) {
            throw new IllegalArgumentException("Weight values must be in the range of [0.0 - 1.0]");
        }

        TreeNode root = treeService.buildTree();
        treeService.validateChildNodesWeightsSum(root, preferences);
    }


    private void processAnalysisReport(AnalysisReport analysisReport,
                                       Map<String, Double> detectedLanguages,
                                       String projectUrl) {
        /*
            Converting 476af562-93da-47e4-a553-08c3173be0ac:graph.py -> graph.py
         */
        for (IssuesReport.IssueDetails issue : analysisReport.getIssuesReport().getIssues()) {
            String component = issue.getComponent().split(":")[1];
            issue.setComponent(component);
        }

        /*
            Converting 476af562-93da-47e4-a553-08c3173be0ac:graph.py -> graph.py
         */
        for (HotspotsReport.HotspotDetails hotspot : analysisReport.getHotspotsReport().getHotspots()) {
            String component = hotspot.getComponent().split(":")[1];
            hotspot.setComponent(component);
        }

        Map<QualityMetric, Double> updatedQualityMetricsReport = metricService.applyUtf(
                analysisReport.getQualityMetricsReport(),
                analysisReport.getIssuesReport(),
                analysisReport.getHotspotsReport());

        analysisReport.setLanguages(detectedLanguages);
        analysisReport.setQualityMetricsReport(updatedQualityMetricsReport);
        analysisReport.setProjectUrl(Link.of(projectUrl));
    }

    private boolean isMavenProject(Path project) {
        try (Stream<Path> paths = Files.walk(project)) {
            return paths.anyMatch(p -> p.getFileName().toString().equals("pom.xml"));
        } catch (IOException e) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    private void analyzeMavenProject(String projectKey, String projectPath) {
        try {
            dockerService.analyzeMavenProject(projectKey, projectPath);
        } catch (IOException ioe) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    private int saveAnalysis(Integer userId) {
        return analysisRepository.saveAnalysis(new Analysis(userId, LocalDate.now()));
    }

    private void saveAllAnalysisReports(Integer analysisId, List<AnalysisReport> reports) {
        reports.forEach(report -> {
            report.setAnalysisId(analysisId);
            analysisRepository.saveAnalysisReport(report);
        });
    }

    /*
        We are saving the constraints only if they are not null and not empty. In the case when we try to
        retrieve constraints for an analysis that the relevant request had no constraints we will return an empty list.
     */
    private void saveConstraints(Integer analysisId, List<Constraint> constraints) {
        if (constraints != null && !constraints.isEmpty()) {
            constraints.forEach(constraint -> {
                constraint.setAnalysisId(analysisId);
                analysisRepository.saveAnalysisConstraint(constraint);
            });
        }
    }

    /*
        We are saving the preferences only if they are not null and not empty. In the case when we try to
        retrieve preferences for an analysis that the relevant request had no preferences we will return an empty list.
     */
    private void savePreferences(Integer analysisId, List<Preference> preferences) {
        if (preferences != null && !preferences.isEmpty()) {
            preferences.forEach(preference -> {
                preference.setAnalysisId(analysisId);
                analysisRepository.saveAnalysisPreference(preference);
            });
        }
    }

    private Analysis findAnalysisByAnalysisId(Integer analysisId) {
        return analysisRepository.findAnalysisByAnalysisId(analysisId).orElseThrow(() ->
                new ResourceNotFoundException("No analysis was found for analysis id: " + analysisId));
    }

    private List<AnalysisReport> findAnalysisReportsByAnalysisId(Integer analysisId) {
        return analysisRepository.findAnalysisReportsByAnalysisId(analysisId).orElseThrow(() ->
                new ResourceNotFoundException("Analysis reports were not found for analysis with id: " + analysisId));
    }

    /*
        If no constraints were provided during the request, we return an empty list.
    */
    private List<Constraint> findAnalysisConstraintsByAnalysisId(Integer analysisId) {
        return analysisRepository.findAnalysisConstraintsByAnalysisId(analysisId).orElse(
                Collections.emptyList());
    }

    /*
        If no preferences were provided during the request, we return an empty list.
     */
    private List<Preference> findAnalysisPreferencesByAnalysisId(Integer analysisId) {
        return analysisRepository.findAnalysisPreferencesByAnalysisId(analysisId).orElse(
                Collections.emptyList());
    }

    private void validateRefreshRequest(RefreshRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("No refresh request was provided.");
        }

        validateConstraints(request.constraints());
        validatePreferences(request.preferences());
    }
}

