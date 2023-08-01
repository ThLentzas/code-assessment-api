package gr.aegean.service.assessment;

import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JavaType;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class FilteringServiceTest {
    private FilteringService filteringService;

    @Test
    void shouldReturnAnEmptyCompliantList() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new Constraint(QualityMetric.CYCLOMATIC_COMPLEXITY, QualityMetricOperator.GT, 0.85));

        String analysisReportPath = "src/test/resources/reports/analysis-report.json";
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, AnalysisReport.class);
        List<AnalysisReport> reports = mapper.readValue(new File(analysisReportPath), type);

        System.out.println(reports.get(0).getIssuesReport().getIssues().size() + reports.get(0).getHotspotsReport().getHotspots().size());
    }
}
