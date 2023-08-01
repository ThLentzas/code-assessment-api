package gr.aegean.service.assessment;

import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JavaType;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class FilteringServiceTest {
    private FilteringService filteringService;

    @BeforeEach
    void setup() {
        filteringService = new FilteringService();
    }

    @Test
    void shouldReturnAnEmptyCompliantList() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.CYCLOMATIC_COMPLEXITY, QualityMetricOperator.GT, 0.85));

        String analysisReportPath = "src/test/resources/reports/analysis-report.json";
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        List<List<AnalysisReport>> actual = filteringService.filter(reports, constraints);

        /*
            actual.get(0) = compliant list
            actual.get(1) = non-compliant list
         */
        assertThat(actual.get(0)).isEmpty();
        assertThat(actual.get(1)).hasSize(2);
    }

    @Test
    void shouldReturnOneReportInEachList() throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.TECHNICAL_DEBT_RATIO, QualityMetricOperator.GT, 0.95));
        constraints.add(new Constraint(QualityMetric.VULNERABILITY_SEVERITY, QualityMetricOperator.GT, 0.5));

        String analysisReportPath = "src/test/resources/reports/analysis-report.json";
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        List<List<AnalysisReport>> actual = filteringService.filter(reports, constraints);

        /*
            actual.get(0) = compliant list
            actual.get(1) = non-compliant list
         */
        assertThat(actual.get(0)).hasSize(1);
        assertThat(actual.get(1)).hasSize(1);
    }

    @Test
    void shouldReturnAnEmptyNonCompliantList() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.BUG_SEVERITY, QualityMetricOperator.LTE, 1.0));
        constraints.add(new Constraint(QualityMetric.HOTSPOT_PRIORITY, QualityMetricOperator.LTE, 1.0));

        String analysisReportPath = "src/test/resources/reports/analysis-report.json";
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        List<List<AnalysisReport>> actual = filteringService.filter(reports, constraints);

        /*
            actual.get(0) = compliant list
            actual.get(1) = non-compliant list
         */
        assertThat(actual.get(0)).hasSize(2);
        assertThat(actual.get(1)).isEmpty();
    }
}
