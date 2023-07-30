package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;

import java.util.List;

@Getter
public class QualityMetricReport {
    private List<QualityMetricReportDetails> measures;

    @Getter
    public static class QualityMetricReportDetails {
        private String metric;
        private double value;
    }
}
