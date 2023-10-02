package gr.aegean.service.analysis;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;


@Service
public class GitHubService {

    public Optional<Path> cloneProject(File requestFolder, String projectUrl) {
        if (!isValidGitHubUrl(projectUrl)) {
            return Optional.empty();
        }

        /*
            We need two unique ids, 1 for the folder inside Projects and 1 for each repository we download.
            F:\Projects\UUID1\UUID2, F:\Projects\UUID1\UUID3. Each request will have a unique subfolder in the
            Projects folder(UUID1) that will contain all the repositories for that request(UUID2, UUID3).
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

    /*
        Will throw URISyntaxException when it fails to parse the provided url. All cases where non url strings are
        provided.
     */
    private boolean isValidGitHubUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            return host.equals("github.com");
        } catch (URISyntaxException use) {
            return false;
        }
    }
}
