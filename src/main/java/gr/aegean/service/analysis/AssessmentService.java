package gr.aegean.service.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import gr.aegean.entity.Analysis;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import gr.aegean.mapper.dto.AnalysisReportDTOMapper;
import gr.aegean.model.analysis.AnalysisReportDTO;
import gr.aegean.model.analysis.AnalysisResult;
import gr.aegean.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.analysis.AnalysisRequest;
import gr.aegean.entity.AnalysisReport;


@Service
public class AssessmentService {
    private final GitHubService gitHubService;
    private final AnalysisService analysisService;
    private final AuthService authService;
    private final FilteringService filteringService;
    private final RankingService rankingService;
    private final AnalysisReportDTOMapper mapper;
    private final Executor taskExecutor;
    private final File baseDirectory;
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";

    public AssessmentService(GitHubService gitHubService,
                             AnalysisService analysisService,
                             AuthService authService,
                             FilteringService filteringService,
                             RankingService rankingService,
                             AnalysisReportDTOMapper mapper,
                          /*
                                The default one and the one we configured, so we have to use @Qualifier
                           */
                          @Qualifier("taskExecutor") Executor taskExecutor,
                             @Value("${projects.base-directory}") String baseDirectoryPath) {
        this.gitHubService = gitHubService;
        this.analysisService = analysisService;
        this.authService = authService;
        this.filteringService = filteringService;
        this.rankingService = rankingService;
        this.taskExecutor = taskExecutor;
        this.mapper = mapper;
        baseDirectory = new File(baseDirectoryPath);
    }

    public CompletableFuture<Integer> processProject(AnalysisRequest analysisRequest,
                                                     HttpServletRequest httpServletRequest) {
        File requestFolder = new File(baseDirectory + "\\" + UUID.randomUUID());

        if (!requestFolder.mkdir()) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }

        List<CompletableFuture<Optional<AnalysisReport>>> futures = analysisRequest.projectUrls().stream()
                .map(projectUrl -> cloneAndAnalyzeProjectAsync(requestFolder, projectUrl))
                .toList();

        /*
            Wait for all the threads to finish the collect the results remove empty optionals and return the analysis
            report. Empty optionals would have null values in the list.
         */
        CompletableFuture<List<AnalysisReport>> allFutures = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList());

        return allFutures.thenCompose(reports -> {
            /*
                The user submitted repositories that either were all private, only unsupported languages were
                detected or a combination of both.
             */
            if (reports.isEmpty()) {
                throw new IllegalArgumentException("We could not run the analysis. Please ensure that at least one " +
                        "repository is public and uses a supported language.");
            }

            Integer userId = authService.getIdFromSubject(httpServletRequest);
            Integer analysisId = saveAnalysisProcess(userId, reports, analysisRequest);
            /*
                Delete the folder after all the threads are done being processed.
             */
            deleteProjectDirectory(requestFolder);

            return CompletableFuture.completedFuture(analysisId);
        });
    }

    public AnalysisResult findAnalysisResultByAnalysisId(Integer analysisId) {
        List<AnalysisReport> reports = analysisService.findAnalysisReportsByAnalysisId(analysisId);
        List<Preference> preferences = analysisService.findAnalysisPreferenceByAnalysisId(analysisId);
        List<Constraint> constraints = analysisService.findAnalysisConstraintsByAnalysisId(analysisId);

        /*
            No constraints -> no filtering
         */
        if (constraints.isEmpty()) {
            reports = reports.stream()
                    //Map is an alternative.
                    .peek(report ->
                            report.setRank(rankingService.rankTree(report.getQualityMetricsReport(), preferences)))
                    //Descending order based on the rank.
                    .sorted(Comparator.comparing(AnalysisReport::getRank).reversed())
                    .toList();

            List<AnalysisReportDTO> reportDTOS = reports.stream()
                    .map(mapper)
                    .toList();

            return new AnalysisResult(List.of(reportDTOS));
        }

        List<List<AnalysisReport>> filteredReportsList = filteringService.filter(reports, constraints);
        List<List<AnalysisReportDTO>> rankedFilteredReports = new ArrayList<>();

        for(List<AnalysisReport> filteredReports : filteredReportsList) {
            filteredReports = filteredReports.stream()
                    .peek(filteredReport ->
                            filteredReport.setRank(rankingService.rankTree(
                                    filteredReport.getQualityMetricsReport(), preferences)))
                    .sorted(Comparator.comparing(AnalysisReport::getRank).reversed())
                    .toList();

            List<AnalysisReportDTO> filteredReportsDTOS = filteredReports.stream()
                    .map(mapper)
                    .toList();

            rankedFilteredReports.add(filteredReportsDTOS);
        }

        return new AnalysisResult(rankedFilteredReports);
    }

    private CompletableFuture<Optional<AnalysisReport>> cloneAndAnalyzeProjectAsync(File requestFolder,
                                                                                    String projectUrl) {
        return CompletableFuture.supplyAsync(() -> gitHubService.cloneProject(requestFolder, projectUrl)
                .flatMap(projectPath -> analysisService.analyzeProject(projectPath, projectUrl)), taskExecutor);
    }


    private void deleteProjectDirectory(File requestFolder) {
        try {
            FileUtils.deleteDirectory(requestFolder);
        } catch (IOException e) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    /*
        We are saving the entire process once it's done and not a report at a time.
     */
    private Integer saveAnalysisProcess(Integer userId, List<AnalysisReport> reports, AnalysisRequest analysisRequest) {
        Integer analysisId = saveAnalysis(userId);
        saveAllAnalysisReports(analysisId, reports);
        saveConstraint(analysisId, analysisRequest);
        savePreference(analysisId, analysisRequest);

        return analysisId;
    }

    private Integer saveAnalysis(Integer userId) {
        return analysisService.saveAnalysis(new Analysis(userId, LocalDateTime.now()));
    }

    private void saveConstraint(Integer analysisId, AnalysisRequest analysisRequest) {
        /*
            If any constraints are submitted we save them in our db
         */
        if (analysisRequest.constraints() != null && !analysisRequest.constraints().isEmpty()) {
            analysisService.saveConstraint(analysisId, analysisRequest.constraints());
        }
    }

    private void savePreference(Integer analysisId, AnalysisRequest analysisRequest) {
        /*
            If any constraints are submitted we save them in our db
         */
        if (analysisRequest.preferences() != null && !analysisRequest.preferences().isEmpty()) {
            analysisService.savePreference(analysisId, analysisRequest.preferences());
        }
    }

    private void saveAllAnalysisReports(Integer analysisId, List<AnalysisReport> reports) {
        reports.forEach(report -> {
            report.setAnalysisId(analysisId);
            analysisService.saveAnalysisReport(report);
        });
    }
}