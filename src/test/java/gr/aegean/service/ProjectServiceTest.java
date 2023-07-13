package gr.aegean.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import gr.aegean.model.analysis.AnalysisRequest;


@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    /*
        Based on documentation, it will create a single directory path for the entire class.Recommended
     */
    @TempDir
    static Path tempDirectory;
    static String baseDirectoryPath;
    /*
        The download functionality will be tested with IT.
     */
    @Mock
    private GitHubService gitHubService;
    private ProjectService underTest;

    @BeforeAll
    static void beforeAll() {
        baseDirectoryPath = tempDirectory.toAbsolutePath().toString();
    }

    @BeforeEach
    void setup() throws Exception {
        underTest = new ProjectService(baseDirectoryPath, gitHubService);

        /*
            Delete all files. Exclude the base temp directory itself
         */
        Files.walk(tempDirectory)
                .filter(path -> !path.equals(tempDirectory))
                .map(Path::toFile)
                .sorted(Comparator.reverseOrder())
                .forEach(File::delete);
    }

    @Test
    void shouldCloneRepositoriesForValidGitHubUrls() throws GitAPIException {
        AnalysisRequest request = new AnalysisRequest(List.of(
                "https://github.com/user/repo1",
                "https://github.com/user/repo2"));

        Git gitMock = mock(Git.class);
        when(gitHubService.cloneRepository(any(String.class), any(File.class))).thenReturn(gitMock);
        when(gitHubService.isValidGitHubUrl(any(String.class))).thenReturn(true);

        File clonedDir = underTest.cloneProject(request);

        /*
            Since gitService.cloneRepository() creates the subdirectories and now is mocked, we will test that the
            cloned directory has 1 folder for each project with IT.
         */
        assertThat(clonedDir).isDirectory();
        assertThat(clonedDir.toPath())
                .isAbsolute()
                .startsWith(tempDirectory.toAbsolutePath());

        verify(gitHubService, times(2)).cloneRepository(anyString(), any(File.class));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAllTheProvidedUrlsAreNotValidGitHubUrlsOrReposArePrivate() {
        AnalysisRequest request = new AnalysisRequest(List.of(
                "https://test.com/user",
                "https://test.com"));

        when(gitHubService.isValidGitHubUrl(any(String.class))).thenReturn(false);

        assertThatThrownBy(() -> underTest.cloneProject(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("All provided URLs are invalid or the repositories are private. Please" +
                        " provide at least one valid GitHub URL for a public repository.");
    }
}
