package gr.aegean.service.assessment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JavaType;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;

import static org.mockito.Mockito.verifyNoInteractions;


@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {
    private AssessmentService underTest;
    @Mock
    private RankingService rankingService;
    @Mock
    private FilteringService filteringService;

    @BeforeEach
    void setup() {
        underTest = new AssessmentService(rankingService, filteringService);
    }

    @Test
    void shouldNotFilterReportsWhenNoConstraintsWereProvided() throws IOException {
        //Arrange
        ObjectMapper mapper = new ObjectMapper();
        List<Constraint> constraints = new ArrayList<>();
        List<Preference> preferences = new ArrayList<>();

        String analysisReportPath = "src/test/resources/reports/analysis-report.json";
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        //Act
        underTest.assessAnalysisResult(reports, constraints, preferences);

        //Assert
        verifyNoInteractions(filteringService);
    }
}
