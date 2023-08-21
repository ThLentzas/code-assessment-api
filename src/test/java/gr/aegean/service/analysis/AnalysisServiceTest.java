package gr.aegean.service.analysis;

import gr.aegean.AbstractTestContainers;
import gr.aegean.entity.Analysis;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import gr.aegean.entity.User;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.model.analysis.RefreshRequest;
import gr.aegean.model.analysis.quality.QualityAttribute;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;
import gr.aegean.repository.AnalysisRepository;
import gr.aegean.repository.EmailUpdateRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.service.assessment.AssessmentService;
import gr.aegean.service.auth.CookieService;
import gr.aegean.service.auth.JwtService;
import gr.aegean.service.email.EmailService;
import gr.aegean.service.user.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JavaType;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest extends AbstractTestContainers {
    @Mock
    private LanguageService languageService;
    @Mock
    private SonarService sonarService;
    @Mock
    private MetricCalculationService metricCalculationService;
    @Mock
    private AssessmentService assessmentService;
    @Mock
    private DockerService dockerService;
    @Mock
    private EmailService emailService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private CookieService cookieService;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailUpdateRepository emailUpdateRepository;
    private UserService userService;
    private AnalysisService underTest;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setup() {
        AnalysisRepository analysisRepository = new AnalysisRepository(
                getJdbcTemplate());
        underTest = new AnalysisService(
                languageService,
                sonarService,
                metricCalculationService,
                assessmentService,
                dockerService,
                analysisRepository);

        UserRepository userRepository = new UserRepository(getJdbcTemplate());
        userService = new UserService(
                cookieService,
                jwtService,
                userRepository,
                emailUpdateRepository,
                emailService,
                analysisService,
                passwordEncoder);

        /*
            There is no need to delete analysis reports, constraints and preferences because the id of the analysis is
            a foreign key with ON DELETE CASCADE.
         */
        analysisRepository.deleteAllAnalyses();
        userRepository.deleteAllUsers();
    }

    /*
        We cant mock the UserService. We have to have a user in our db, in order to associate the userId foreign key in
        the analysis table.
     */
    @Test
    void shouldSaveAnalysisProcess() throws IOException {
        //Arrange
        User user = generateUser();
        Integer userId = userService.registerUser(user);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.TECHNICAL_DEBT_RATIO, QualityMetricOperator.GT, 0.95));
        constraints.add(new Constraint(QualityMetric.VULNERABILITY_SEVERITY, QualityMetricOperator.GT, 0.5));

        List<Preference> preferences = new ArrayList<>();
        preferences.add(new Preference(QualityAttribute.SIMPLICITY, 0.34));
        preferences.add(new Preference(QualityAttribute.SECURITY_REMEDIATION_EFFORT, 0.25));

        ObjectMapper mapper = new ObjectMapper();
        String analysisReportPath = "src/test/resources/reports/analysis-report.json";
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        //Act
        Integer actual = underTest.saveAnalysisProcess(userId, reports, constraints, preferences);

        //Assert
        assertThat(actual).isNotNull().isPositive();
    }

    @Test
    void shouldFindAnalysesByUserId() throws IOException {
        //Arrange
        User user = generateUser();
        Integer userId = userService.registerUser(user);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.TECHNICAL_DEBT_RATIO, QualityMetricOperator.GT, 0.95));
        constraints.add(new Constraint(QualityMetric.VULNERABILITY_SEVERITY, QualityMetricOperator.GT, 0.5));

        List<Preference> preferences = new ArrayList<>();
        preferences.add(new Preference(QualityAttribute.SIMPLICITY, 0.34));
        preferences.add(new Preference(QualityAttribute.SECURITY_REMEDIATION_EFFORT, 0.25));

        ObjectMapper mapper = new ObjectMapper();
        String analysisReportPath = "src/test/resources/reports/analysis-report.json";
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);
        underTest.saveAnalysisProcess(userId, reports, constraints, preferences);

        //Act
        List<Analysis> actual = underTest.getHistory(userId);

        //Assert
        assertThat(actual).hasSize(1);
    }

    @Test
    void shouldDeleteAnalysis() throws IOException {
        //Arrange
        User user = generateUser();
        Integer userId = userService.registerUser(user);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.TECHNICAL_DEBT_RATIO, QualityMetricOperator.GT, 0.95));
        constraints.add(new Constraint(QualityMetric.VULNERABILITY_SEVERITY, QualityMetricOperator.GT, 0.5));

        List<Preference> preferences = new ArrayList<>();
        preferences.add(new Preference(QualityAttribute.SIMPLICITY, 0.34));
        preferences.add(new Preference(QualityAttribute.SECURITY_REMEDIATION_EFFORT, 0.25));

        ObjectMapper mapper = new ObjectMapper();
        String analysisReportPath = "src/test/resources/reports/analysis-report.json";
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);
        Integer analysisId = underTest.saveAnalysisProcess(userId, reports, constraints, preferences);

        //Act
        underTest.deleteAnalysis(analysisId, userId);

        //Assert
        assertThatThrownBy(() -> underTest.findAnalysisResultByAnalysisId(analysisId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No analysis was found for analysis id: " + analysisId);
    }

    @ParameterizedTest
    @NullSource
    void shouldThrowIllegalArgumentExceptionWhenRefreshRequestIsNull(RefreshRequest request) {
        //Arrange Act Assert
        assertThatThrownBy(() -> underTest.refreshAnalysisResult(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No refresh request was provided.");
    }

    private User generateUser() {
        return User.builder()
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("test"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();
    }

}
