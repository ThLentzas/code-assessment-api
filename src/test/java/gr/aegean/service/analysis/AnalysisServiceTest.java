package gr.aegean.service.analysis;

import gr.aegean.AbstractUnitTest;
import gr.aegean.entity.Analysis;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import gr.aegean.entity.User;
import gr.aegean.exception.ResourceNotFoundException;
import gr.aegean.model.dto.analysis.AnalysisRequest;
import gr.aegean.model.analysis.quality.QualityAttribute;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;
import gr.aegean.repository.AnalysisRepository;
import gr.aegean.repository.EmailUpdateRepository;
import gr.aegean.repository.UserRepository;
import gr.aegean.service.assessment.AssessmentService;
import gr.aegean.service.assessment.TreeService;
import gr.aegean.service.auth.JwtService;
import gr.aegean.service.email.EmailService;
import gr.aegean.service.user.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;


@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest extends AbstractUnitTest {
    @Mock
    private LanguageService languageService;
    @Mock
    private SonarService sonarService;
    @Mock
    private MetricService metricService;
    @Mock
    private AssessmentService assessmentService;
    @Mock
    private DockerService dockerService;
    @Mock
    private EmailService emailService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private JwtService jwtService;
    @Mock
    private TreeService treeService;
    @Mock
    private EmailUpdateRepository emailUpdateRepository;
    private UserService userService;
    private AnalysisService underTest;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ObjectMapper mapper = new ObjectMapper();

    /*
        The idea is for the below tests is that since the analysis json is big, and it's difficult to set up the scenario
        for each test case we read from the relative files and map them to our objects.
     */
    @BeforeEach
    void setup() {
        AnalysisRepository analysisRepository = new AnalysisRepository(
                getJdbcTemplate());
        underTest = new AnalysisService(
                languageService,
                sonarService,
                metricService,
                assessmentService,
                dockerService,
                jwtService,
                treeService,
                analysisRepository
        );

        UserRepository userRepository = new UserRepository(getJdbcTemplate());
        userService = new UserService(
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
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.registerUser(user);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.TECHNICAL_DEBT_RATIO, QualityMetricOperator.GT, 0.95));
        constraints.add(new Constraint(QualityMetric.VULNERABILITY_SEVERITY, QualityMetricOperator.GT, 0.5));

        List<Preference> preferences = new ArrayList<>();
        preferences.add(new Preference(QualityAttribute.SIMPLICITY, 0.34));
        preferences.add(new Preference(QualityAttribute.SECURITY_REMEDIATION_EFFORT, 0.25));

        String analysisReportPath = "src/test/resources/reports/analysis-reports.json";
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        //Act
        Integer actual = underTest.saveAnalysisProcess(user.getId(), reports, constraints, preferences);

        //Assert
        assertThat(actual).isPositive();
    }

    @Test
    void shouldGetUserHistory() throws IOException {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.registerUser(user);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.TECHNICAL_DEBT_RATIO, QualityMetricOperator.GT, 0.95));
        constraints.add(new Constraint(QualityMetric.VULNERABILITY_SEVERITY, QualityMetricOperator.GT, 0.5));

        List<Preference> preferences = new ArrayList<>();
        preferences.add(new Preference(QualityAttribute.SIMPLICITY, 0.34));
        preferences.add(new Preference(QualityAttribute.SECURITY_REMEDIATION_EFFORT, 0.25));

        String analysisReportPath = "src/test/resources/reports/analysis-reports.json";
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        underTest.saveAnalysisProcess(user.getId(), reports, constraints, preferences);

        //Act
        List<Analysis> actual = underTest.getHistory(user.getId());

        //Assert
        assertThat(actual).hasSize(1);
    }

    @Test
    void shouldDeleteAnalysis() throws IOException {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.registerUser(user);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.TECHNICAL_DEBT_RATIO, QualityMetricOperator.GT, 0.95));
        constraints.add(new Constraint(QualityMetric.VULNERABILITY_SEVERITY, QualityMetricOperator.GT, 0.5));

        List<Preference> preferences = new ArrayList<>();
        preferences.add(new Preference(QualityAttribute.SIMPLICITY, 0.34));
        preferences.add(new Preference(QualityAttribute.SECURITY_REMEDIATION_EFFORT, 0.25));

        String analysisReportPath = "src/test/resources/reports/analysis-reports.json";
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        Integer analysisId = underTest.saveAnalysisProcess(user.getId(), reports, constraints, preferences);

        when(jwtService.getSubject()).thenReturn(user.getId().toString());

        //Act
        underTest.deleteAnalysis(analysisId);

        //Assert
        assertThatThrownBy(() -> underTest.findAnalysisResultByAnalysisId(analysisId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No analysis was found for analysis id: " + analysisId);
    }

    @Test
    void shouldGetAnalysisRequest() throws IOException {
        //Arrange
        User user = generateUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.registerUser(user);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.TECHNICAL_DEBT_RATIO, QualityMetricOperator.GT, 0.95));
        constraints.add(new Constraint(QualityMetric.VULNERABILITY_SEVERITY, QualityMetricOperator.GT, 0.5));

        List<Preference> preferences = new ArrayList<>();
        preferences.add(new Preference(QualityAttribute.SIMPLICITY, 0.34));
        preferences.add(new Preference(QualityAttribute.SECURITY_REMEDIATION_EFFORT, 0.25));

        String analysisReportPath = "src/test/resources/reports/analysis-reports.json";
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        Integer analysisId = underTest.saveAnalysisProcess(user.getId(), reports, constraints, preferences);

        List<String> projectUrls = reports.stream()
                .map(report -> report.getProjectUrl().getHref())
                .toList();
        AnalysisRequest expected = new AnalysisRequest(projectUrls, constraints, preferences);

        //Act
        AnalysisRequest actual = underTest.findAnalysisRequestByAnalysisId(analysisId);

        //Assert
        assertThat(actual).isEqualTo(expected);

    }

    private User generateUser() {
        return User.builder()
                .firstname("Test")
                .lastname("Test")
                .username("TestT")
                .email("test@example.com")
                .password(passwordEncoder.encode("IgwcUQAlfX$E"))
                .bio("I have a real passion for teaching")
                .location("Cleveland, OH")
                .company("Code Monkey, LLC")
                .build();
    }
}
