package gr.aegean.service;

import gr.aegean.exception.ServerErrorException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;


@Service
public class GitService {
    public Git cloneRepository(String uri, File directory) throws GitAPIException {
        return Git.cloneRepository()
                .setURI(uri)
                .setDirectory(directory)
                .call();
    }

    public boolean isValidGitHubUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            return host.startsWith("https://") && host.equals("github.com");
        } catch (Exception e) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }
}
