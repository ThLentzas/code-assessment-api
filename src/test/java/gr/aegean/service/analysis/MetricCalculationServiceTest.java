package gr.aegean.service.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JavaType;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.sonarqube.HotspotsReport;
import gr.aegean.model.analysis.sonarqube.IssuesReport;

import static org.assertj.core.api.Assertions.assertThat;


class MetricCalculationServiceTest {
    private MetricCalculationService underTest;

    @BeforeEach
    void setup() {
        underTest = new MetricCalculationService();
    }

    @Test
    void shouldApplyUtf() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String issuesReportPath = "src/test/resources/reports/issues-report.json";
        String hotspotsReportPath = "src/test/resources/reports/hotspots-report.json";
        String metricsReportPath = "src/test/resources/reports/metrics-report.json";

        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, IssuesReport.IssueDetails.class);
        List<IssuesReport.IssueDetails> issuesDetails = mapper.readValue(new File(issuesReportPath), type);
        type = mapper.getTypeFactory().constructCollectionType(List.class, HotspotsReport.HotspotDetails.class);
        List<HotspotsReport.HotspotDetails> hotspotsDetails = mapper.readValue(new File(hotspotsReportPath), type);
        type = mapper.getTypeFactory().constructMapType(EnumMap.class, QualityMetric.class, Double.class);
        EnumMap<QualityMetric, Double> metricsReport = mapper.readValue(new File(metricsReportPath), type);
        EnumMap<QualityMetric, Double> expected = new EnumMap<>(QualityMetric.class);

        expected.put(QualityMetric.COMMENT_RATE, 0.6662906694752284);
        expected.put(QualityMetric.METHOD_SIZE, 1.0);
        expected.put(QualityMetric.DUPLICATION, 1.0);
        expected.put(QualityMetric.BUG_SEVERITY, 1.0);
        expected.put(QualityMetric.TECHNICAL_DEBT_RATIO, 0.991);
        expected.put(QualityMetric.RELIABILITY_REMEDIATION_EFFORT, 1.0);
        expected.put(QualityMetric.CYCLOMATIC_COMPLEXITY, 0.7755834829443446);
        expected.put(QualityMetric.COGNITIVE_COMPLEXITY, 0.6122082585278277);
        expected.put(QualityMetric.VULNERABILITY_SEVERITY, 1.0);
        expected.put(QualityMetric.HOTSPOT_PRIORITY, 0.041353383458646614);
        expected.put(QualityMetric.SECURITY_REMEDIATION_EFFORT, 1.0);

        Map<QualityMetric, Double> actual = underTest.applyUtf(metricsReport, issuesDetails, hotspotsDetails);

        /*
            Will fail if the expected and actual maps are not exactly equal, meaning they contain the same keys with
            the same values.
         */
        assertThat(actual).isEqualTo(expected);
    }
}
