package gr.aegean.model.analysis.sonarqube;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
public class QualityMetricReport {
    private List<QualityMetricReportDetails> measures;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QualityMetricReportDetails {
        private String metric;
        private double value;
    }
}
