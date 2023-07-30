package gr.aegean.service;

import gr.aegean.service.analysis.*;
import gr.aegean.service.assessment.AssessmentService;
import gr.aegean.service.auth.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class AsyncServiceTest {
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
    @Mock
    private AnalysisService analysisService;
    @Mock
    private AuthService authService;
    @Mock
    private AssessmentService assessmentService;
    @Mock
    private Executor taskExecutor;
    private AsyncService underTest;

    @BeforeAll
    static void beforeAll() {
        baseDirectoryPath = tempDirectory.toAbsolutePath().toString();
    }

    @BeforeEach
    void setup() throws Exception {
        underTest = new AsyncService(
                gitHubService,
                analysisService,
                authService,
                assessmentService,
                taskExecutor,
                baseDirectoryPath);

        /*
            Delete all files. Exclude the base temp directory itself
         */
        Files.walk(tempDirectory)
                .filter(path -> !path.equals(tempDirectory))
                .map(Path::toFile)
                .sorted(Comparator.reverseOrder())
                .forEach(File::delete);
    }
}