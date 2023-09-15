package gr.aegean.service.analysis;

import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import gr.aegean.model.analysis.quality.QualityAttribute;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.TreeNode;
import gr.aegean.service.assessment.TreeService;
import gr.aegean.service.auth.JwtService;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.dto.analysis.AnalysisRequest;
import gr.aegean.entity.AnalysisReport;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;


@Service
public class AsyncService {
    private final GitHubService gitHubService;
    private final AnalysisService analysisService;
    private final JwtService jwtService;
    private final TreeService treeService;
    private final Executor taskExecutor;
    private final File baseDirectory;
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";

    public AsyncService(GitHubService gitHubService,
                        AnalysisService analysisService,
                        JwtService jwtService,
                        TreeService treeService,
                        /*
                            The default one and the one we configured, so we have to use @Qualifier
                         */
                        @Qualifier("taskExecutor") Executor taskExecutor,
                        @Value("${projects.base-directory}") String baseDirectoryPath) {
        this.gitHubService = gitHubService;
        this.analysisService = analysisService;
        this.jwtService = jwtService;
        this.treeService = treeService;
        this.taskExecutor = taskExecutor;
        baseDirectory = new File(baseDirectoryPath);
    }

    /*
        We have to validate constraints and preferences at the start of the request because an invalid
        constraint/preference means that the analysis will not happen. Validating the project urls happens later.
        Even if all the project urls are valid, they can still be private repos which also means that the analysis will
        not happen.
     */
    public CompletableFuture<Integer> processProject(AnalysisRequest analysisRequest,
                                                     HttpServletRequest httpServletRequest) {
        validateConstraints(analysisRequest.constraints());
        validatePreferences(analysisRequest.preferences());

        File requestFolder = new File(baseDirectory + File.separator + UUID.randomUUID());
        if (!requestFolder.mkdir()) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }

        List<CompletableFuture<Optional<AnalysisReport>>> futures = analysisRequest.projectUrls().stream()
                .map(projectUrl -> cloneAndAnalyzeProjectAsync(requestFolder, projectUrl))
                .toList();

        /*
            Wait for all the threads to finish to collect the results, remove empty optionals and return the analysis
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

            Integer userId = Integer.parseInt(jwtService.getSubject(httpServletRequest));
            Integer analysisId = saveAnalysisProcess(userId, reports, analysisRequest);
            /*
                Delete the folder after all the threads are done being processed.
             */
            deleteProjectDirectory(requestFolder);

            return CompletableFuture.completedFuture(analysisId);
        });
    }

    private CompletableFuture<Optional<AnalysisReport>> cloneAndAnalyzeProjectAsync(File requestFolder,
                                                                                    String projectUrl) {
        /*
            This is the async part, where we first download a project url, then we analyze it. We do that for every
            project url
         */
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
        Saving the entire process once it's done and not a report at a time.
     */
    private Integer saveAnalysisProcess(Integer userId, List<AnalysisReport> reports, AnalysisRequest analysisRequest) {
        return analysisService.saveAnalysisProcess(
                userId,
                reports,
                analysisRequest.constraints(),
                analysisRequest.preferences());
    }

     /*
        We don't have to check if we have invalid quality metric values, it will be handled by the deserializer during
        the deserialization.
     */
    private void validateConstraints(List<Constraint> constraints) {
        Set<QualityMetric> qualityMetrics = constraints.stream()
                .map(Constraint::getQualityMetric)
                .collect(Collectors.toSet());

        /*
            Case: Duplicate quality metric was provided.
         */
        if(qualityMetrics.size() != constraints.size()) {
            throw new IllegalArgumentException("Invalid constraint values. Avoid duplicates");
        }

        /*
            All threshold values must be in the range of [0.0 - 1.0]
         */
        boolean isValidThreshold = constraints.stream()
                .allMatch(constraint -> constraint.getThreshold() <= 1.0 && constraint.getThreshold() >= 0);
        if(!isValidThreshold) {
            throw new IllegalArgumentException("Threshold values must be in the range of [0.0 - 1.0]");
        }
    }

    /*
        We don't have to check if we have invalid quality attribute values, it will be handled by the deserializer
        during the deserialization.
     */
    private void validatePreferences(List<Preference> preferences) {
        Set<QualityAttribute> qualityAttributes = preferences.stream()
                .map(Preference::getQualityAttribute)
                .collect(Collectors.toSet());

        /*
            Case: Duplicate quality attribute was provided.
         */
        if(qualityAttributes.size() != preferences.size()) {
            throw new IllegalArgumentException("Invalid preference values. Avoid duplicates");
        }

        /*
            All weight values must be in the range of [0.0 - 1.0]
         */
        boolean isValidWeight = preferences.stream()
                .allMatch(preference -> preference.getWeight() <= 1.0 && preference.getWeight() >= 0);
        if(!isValidWeight) {
            throw new IllegalArgumentException("Weight values must be in the range of [0.0 - 1.0]");
        }

        TreeNode root = treeService.buildTree();
        treeService.validateChildNodesWeightsSum(root, preferences);
    }
}