package gr.aegean.service.analysis;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.sonarqube.HotspotsReport;
import gr.aegean.model.analysis.sonarqube.IssuesReport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JavaType;

import static org.assertj.core.api.Assertions.assertThat;


class MetricServiceTest {
    private MetricService underTest;

    @BeforeEach
    void setup() {
        underTest = new MetricService();
    }

    @Test
    void shouldApplyUtf() throws IOException {
        //Arrange
        ObjectMapper mapper = new ObjectMapper();
        String issuesReportPath = "src/test/resources/reports/issues-report.json";
        String hotspotsReportPath = "src/test/resources/reports/hotspots-report.json";
        String metricsReportPath = "src/test/resources/reports/metrics-report.json";

        IssuesReport issuesReport = mapper.readValue(new File(issuesReportPath), IssuesReport.class);
        HotspotsReport hotspotsReport = mapper.readValue(new File(hotspotsReportPath), HotspotsReport.class);
        JavaType type = mapper.getTypeFactory().constructMapType(Map.class, QualityMetric.class, Double.class);
        Map<QualityMetric, Double> metricsReport = mapper.readValue(new File(metricsReportPath), type);
        Map<QualityMetric, Double> expected = new EnumMap<>(QualityMetric.class);

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

        //Act
        Map<QualityMetric, Double> actual = underTest.applyUtf(metricsReport, issuesReport, hotspotsReport);

        /*
            Assert
            Will fail if the expected and actual maps are not exactly equal, meaning they contain the same keys with
            the same values.
         */
        assertThat(actual).isEqualTo(expected);
    }
}
