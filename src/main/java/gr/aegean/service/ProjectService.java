package gr.aegean.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import gr.aegean.model.entity.AnalysisReport;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.analysis.AnalysisRequest;


@Service
public class ProjectService {
    private final GitHubService gitHubService;
    private final AnalysisService analysisService;
    private final File baseDirectory;
    private final Executor taskExecutor;
    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    public ProjectService(
            @Value("${projects.base-directory}") String baseDirectoryPath,
            GitHubService gitHubService,
            AnalysisService analysisService,
            /*
                The default one and the one we configured, so we have to use @Qualifier
             */
            @Qualifier("taskExecutor") Executor taskExecutor) {
        this.gitHubService = gitHubService;
        this.analysisService = analysisService;
        this.taskExecutor = taskExecutor;
        baseDirectory = new File(baseDirectoryPath);
    }

    public CompletableFuture<List<AnalysisReport>> processProject(AnalysisRequest request) {
        File requestFolder = new File(baseDirectory + "\\" + UUID.randomUUID());

        if (!requestFolder.mkdir()) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        List<CompletableFuture<AnalysisReport>> futures = request.projectUrls().stream()
                .map(url -> cloneAndAnalyzeProjectAsync(requestFolder, url))
                .toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        CompletableFuture<List<AnalysisReport>> myList = allFutures.thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());

        /*
            Delete the folder after all the threads are done being processed.
         */
        return myList.whenComplete((list, throwable) -> {
            try {
                deleteProjectDirectory(requestFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<AnalysisReport> cloneAndAnalyzeProjectAsync(File requestFolder, String projectUrl) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            logger.info("Processing URL: " + projectUrl);

            // Your code block
            Optional<Path> project = cloneProject(requestFolder, projectUrl);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Optional<AnalysisReport> report = project.flatMap(analysisService::analyzeProjects);

            long endTime = System.nanoTime();
            long durationInNano = (endTime - startTime);  // Total execution time in nano seconds
            long durationInMillis = TimeUnit.MILLISECONDS.toSeconds(durationInNano);  // Convert to milliseconds if needed

            System.out.println(Thread.currentThread().getName() + " - Execution time in milliseconds: " + durationInMillis);
            logger.info("Finished processing URL: " + projectUrl + ", projectFile path: " + project.get());

            return report.orElseThrow(() -> new ServerErrorException("The server encountered an internal error and was unable " +
                    "to complete your request. Please try again later."));
        }, taskExecutor);
    }


    /*
        It was refactored for parallel processing. It processes a project's url at a time and not the entire least of
        urls.
     */
    private Optional<Path> cloneProject(File requestFolder, String projectUrl) {
        /*
            Have a message saying that if in the analysis report they don't see a repository from those they
            provided, it wasn't a valid GitHub repository URL, or it was a private one, or the language was not
            supported.
         */
        if (!gitHubService.isValidGitHubUrl(projectUrl)) {
            return Optional.empty();
        }

        /*
            We need two unique ids, 1 for the folder inside Projects and 1 for each repository we download.
            F:\Projects\UUID1\UUID2, F:\Projects\UUID1\UUID3. Each request will have a unique subfolder in the
            Projects folder that will contain all the repositories for that request.
        */
        File projectFile = new File(requestFolder, UUID.randomUUID().toString());
        try (Git git = gitHubService.cloneRepository(
                projectUrl,
                projectFile)) {
            logger.info("Cloned repository: " + projectUrl + " to path: " + projectFile.getAbsolutePath());
        } catch (GitAPIException e) {
            return Optional.empty();
        }

        return Optional.of(projectFile.toPath());
    }

    public void deleteProjectDirectory(File requestFolder) throws IOException {
        FileUtils.deleteDirectory(requestFolder);
    }
}