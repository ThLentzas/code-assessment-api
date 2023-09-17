package gr.aegean.service.analysis;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.stereotype.Service;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.dto.analysis.AnalysisRequest;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.service.auth.JwtService;


@Service
public class AsyncService {
    private final GitHubService gitHubService;
    private final AnalysisService analysisService;
    private final JwtService jwtService;
    private final Executor taskExecutor;
    private final File baseDirectory;
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";

    /*
        We have to use a DelegatingSecurityContextExecutor() because the separate threads that will run for the
        analysis of each project needs to know the SecurityContextHolder authentication object from the original request
        containing the jwt otherwise the SecurityContextHolder is null for each child thread.
        By using DelegatingSecurityContextExecutor() we automatically propagate the SecurityContext from the parent
        thread to the child thread
     */
    public AsyncService(GitHubService gitHubService,
                        AnalysisService analysisService,
                        JwtService jwtService,
                        /*
                            The default one and the one we configured, so we have to use @Qualifier
                         */
                        @Qualifier("taskExecutor") Executor taskExecutor,
                        @Value("${projects.base-directory}") String baseDirectoryPath) {
        this.gitHubService = gitHubService;
        this.analysisService = analysisService;
        this.jwtService = jwtService;
        this.taskExecutor = new DelegatingSecurityContextExecutor(taskExecutor);
        baseDirectory = new File(baseDirectoryPath);
    }

    /*
        We have to validate constraints and preferences at the start of the request because an invalid
        constraint/preference means that the analysis will not happen. Validating the project urls happens later.
        Even if all the project urls are valid, they can still be private repos which also means that the analysis will
        not happen.
     */
    public CompletableFuture<Integer> processProject(AnalysisRequest analysisRequest) {
        analysisService.validateConstraints(analysisRequest.constraints());
        analysisService.validatePreferences(analysisRequest.preferences());

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
                        "repository is public and uses a supported language");
            }

            Integer userId = Integer.parseInt(jwtService.getSubject());
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
                .flatMap(projectPath -> analysisService.analyze(projectPath, projectUrl)), taskExecutor);
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
}