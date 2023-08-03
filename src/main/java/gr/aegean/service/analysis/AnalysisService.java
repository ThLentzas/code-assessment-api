package gr.aegean.service.analysis;

import gr.aegean.entity.Analysis;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.mapper.dto.AnalysisReportDTOMapper;
import gr.aegean.model.analysis.AnalysisReportDTO;
import gr.aegean.model.analysis.AnalysisResult;
import gr.aegean.model.analysis.RefreshRequest;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.repository.AnalysisRepository;
import gr.aegean.service.assessment.AssessmentService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final LanguageService languageService;
    private final SonarService sonarService;
    private final MetricCalculationService metricCalculationService;
    private final AssessmentService assessmentService;
    private final DockerService dockerService;
    private final AnalysisRepository analysisRepository;
    private final AnalysisReportDTOMapper mapper;
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

        if (detectedLanguages.containsKey("Java")) {
            if (isMavenProject(projectPath)) {
                analyzeMavenProject(projectKey, projectPath.toString());
            } else {
                return Optional.empty();
            }
        } else {
            sonarService.analyzeProject(projectKey, projectPath.toString());
        }

        analysisReport = sonarService.fetchAnalysisReport(projectKey, detectedLanguages);
        Map<QualityMetric, Double> updatedQualityMetricsReport = metricCalculationService.applyUtf(
                analysisReport.getQualityMetricsReport(),
                analysisReport.getIssuesReport().getIssues(),
                analysisReport.getHotspotsReport().getHotspots());

        analysisReport.setQualityMetricsReport(updatedQualityMetricsReport);
        analysisReport.setProjectUrl(Link.of(projectUrl));

        return Optional.of(analysisReport);
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

    public AnalysisResult findAnalysisResultByAnalysisId(Integer analysisId) {
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

        List<List<AnalysisReportDTO>> rankedReportsDTO = mapToDTO(rankedReports);

        return new AnalysisResult(analysis.getId(), rankedReportsDTO, analysis.getCreatedDate());
    }

    public AnalysisReportDTO findAnalysisReportById(Integer reportId) {
        AnalysisReport report = analysisRepository.findAnalysisReportByReportId(reportId).orElseThrow(() ->
                new ResourceNotFoundException("Analysis report was not found for analysis with id" + reportId));

        return mapper.apply(report);
    }

    public AnalysisResult refreshAnalysisResult(Integer analysisId, RefreshRequest request) {
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
            initial analysis, so we delete first the old constraints/preferences and then save the new
            ones.
         */
        analysisRepository.deleteConstraintByAnalysisId(analysisId);
        analysisRepository.deletePreferenceByAnalysisId(analysisId);
        saveConstraints(analysisId, constraints);
        savePreferences(analysisId, preferences);

        List<List<AnalysisReportDTO>> rankedReportsDTO = mapToDTO(rankedReports);

        return new AnalysisResult(analysis.getId(), rankedReportsDTO, analysis.getCreatedDate());
    }

    /*
        Returns the entire user's history
     */
    public List<Analysis> getHistory(Integer userId) {
        return analysisRepository.getHistory(userId).orElse(Collections.emptyList());
    }

    /*
        Returns the user's history in a date range.
     */
    public List<Analysis> getHistoryInDateRange(Integer userId, Date from, Date to) {
        return analysisRepository.getHistoryInDateRange(userId, from, to).orElse(Collections.emptyList());
    }


    public void deleteAnalysis(Integer analysisId, Integer userId) {
        analysisRepository.deleteAnalysis(analysisId, userId);
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

    private void saveConstraints(Integer analysisId, List<Constraint> constraints) {
        if (constraints != null && !constraints.isEmpty()) {
            constraints.forEach(constraint -> {
                constraint.setAnalysisId(analysisId);
                analysisRepository.saveAnalysisConstraint(constraint);
            });
        }
    }

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

    private List<Constraint> findAnalysisConstraintsByAnalysisId(Integer analysisId) {
        return analysisRepository.findAnalysisConstraintsByAnalysisId(analysisId).orElse(
                Collections.emptyList());
    }

    private List<Preference> findAnalysisPreferencesByAnalysisId(Integer analysisId) {
        return analysisRepository.findAnalysisPreferencesByAnalysisId(analysisId).orElse(
                Collections.emptyList());
    }

    private List<List<AnalysisReportDTO>> mapToDTO(List<List<AnalysisReport>> reports) {
        return reports.stream()
                .map(list -> list.stream()
                        .map(mapper)
                        .toList())
                .toList();
    }

    private void validateRefreshRequest(RefreshRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("No refresh request was provided.");
        }
    }
}

