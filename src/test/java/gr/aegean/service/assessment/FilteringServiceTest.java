package gr.aegean.service.assessment;

import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import static org.assertj.core.api.Assertions.assertThat;


class FilteringServiceTest {
    private FilteringService underTest;

    @BeforeEach
    void setup() {
        underTest = new FilteringService();
    }

    @Test
    void shouldReturnAnEmptyCompliantListWhenAllReportsAreNonCompliantWithTheConstraints() throws IOException {
        //Arrange
        ObjectMapper mapper = new ObjectMapper();
        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.CYCLOMATIC_COMPLEXITY, QualityMetricOperator.GT, 0.85));

        String analysisReportPath = "src/test/resources/reports/analysis-reports.json";
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        //Act
        List<List<AnalysisReport>> actual = underTest.filter(reports, constraints);

        /*
            Assert
            actual.get(0) = compliant list
            actual.get(1) = non-compliant list
         */
        assertThat(actual.get(0)).isEmpty();
        assertThat(actual.get(1)).hasSize(reports.size());
    }

    @Test
    void shouldReturnReportsInBothLists() throws IOException {
        //Arrange
        ObjectMapper mapper = new ObjectMapper();
        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.TECHNICAL_DEBT_RATIO, QualityMetricOperator.GT, 0.95));
        constraints.add(new Constraint(QualityMetric.VULNERABILITY_SEVERITY, QualityMetricOperator.GT, 0.5));

        String analysisReportPath = "src/test/resources/reports/analysis-reports.json";
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        //Act
        List<List<AnalysisReport>> actual = underTest.filter(reports, constraints);

        /*
            Assert
            actual.get(0) = compliant list
            actual.get(1) = non-compliant list
         */
        assertThat(actual.get(0)).hasSize(1);
        assertThat(actual.get(1)).hasSize(1);
    }

    @Test
    void shouldReturnAnEmptyNonCompliantListWhenAllReportsAreCompliantWithTheConstraints() throws IOException {
        //Assert
        ObjectMapper mapper = new ObjectMapper();
        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.BUG_SEVERITY, QualityMetricOperator.LTE, 1.0));
        constraints.add(new Constraint(QualityMetric.HOTSPOT_PRIORITY, QualityMetricOperator.LTE, 1.0));

        String analysisReportPath = "src/test/resources/reports/analysis-reports.json";
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        //Act
        List<List<AnalysisReport>> actual = underTest.filter(reports, constraints);

        /*
            Assert
            actual.get(0) = compliant list
            actual.get(1) = non-compliant list
         */
        assertThat(actual.get(0)).hasSize(reports.size());
        assertThat(actual.get(1)).isEmpty();
    }
}
