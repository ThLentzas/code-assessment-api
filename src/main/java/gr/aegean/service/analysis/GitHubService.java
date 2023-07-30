package gr.aegean.service.analysis;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import gr.aegean.exception.ServerErrorException;


@Service
public class GitHubService {

    public Optional<Path> cloneProject(File requestFolder, String projectUrl) {
        if (!isValidGitHubUrl(projectUrl)) {
            return Optional.empty();
        }

        /*
            We need two unique ids, 1 for the folder inside Projects and 1 for each repository we download.
            F:\Projects\UUID1\UUID2, F:\Projects\UUID1\UUID3. Each request will have a unique subfolder in the
            Projects folder that will contain all the repositories for that request.
        */
        File projectFile = new File(requestFolder, UUID.randomUUID().toString());
        try (Git git = cloneRepository(projectUrl, projectFile)) {
            /*
                Exception will be thrown when repository is private.
             */
        } catch (GitAPIException gae) {
            return Optional.empty();
        }

        return Optional.of(projectFile.toPath());
    }
    
    private Git cloneRepository(String uri, File directory) throws GitAPIException {
        return Git.cloneRepository()
                .setURI(uri)
                .setDirectory(directory)
                .call();
    }

    // TODO: 7/30/2023 make this private, once you figure out how to test it in an IT. 
    public boolean isValidGitHubUrl(String url) {
        if(url == null || url.isBlank()) {
            return false;
        }

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
