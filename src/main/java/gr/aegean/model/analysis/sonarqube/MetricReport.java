package gr.aegean.model.analysis.sonarqube;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
public class MetricReport {
    private List<MetricDetails> measures;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetricDetails {
        private String metric;
        private double value;
    }
}
