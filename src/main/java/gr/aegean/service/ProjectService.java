package gr.aegean.service;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.analysis.AnalysisRequest;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    public File cloneProject(AnalysisRequest request) {
        List<String> failedUrls = new ArrayList<>();
        File requestFolder = new File("F:\\Projects\\" + UUID.randomUUID());

        if (!requestFolder.mkdir()) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        for (String projectUrl : request.projectUrls()) {
            /*
                Have a message saying that if in the analysis report they don't see a repository from those that they
                provided, it wasn't a valid GitHub repository URL, or it was a private one.
             */
            if (!isValidGitHubUrl(projectUrl)) {
                failedUrls.add(projectUrl);

                continue;
            }

            /*
                We need two unique ids, 1 for the folder inside Projects and 1 for each repository we download.
                F:\Projects\UUID1\UUID2, F:\Projects\UUID1\UUID3. Each request will have a unique subfolder in the
                 Projects folder that will contain all the repositories for that request.
             */
            try (Git git = Git.cloneRepository()
                    .setURI(projectUrl)
                    .setDirectory(new File(requestFolder + "\\" + UUID.randomUUID())).call()) {
            } catch (GitAPIException e) {

                /*
                    The repository is private
                 */
                failedUrls.add(projectUrl);
            }
        }

        //User didn't provide any valid GitHub repository URLs.
        if (failedUrls.size() == request.projectUrls().size()) {
            throw new IllegalArgumentException("All provided URLs are invalid or the repositories are private. Please" +
                    " provide at least one valid GitHub URL for a public repository.");
        }

        return requestFolder;
    }

    private boolean isValidGitHubUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            return host.equals("github.com");
        } catch (Exception e) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }
}