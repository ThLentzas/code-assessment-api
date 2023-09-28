package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class QualityMetricReport {
    private List<Measures> measures;

    @Getter
    @Setter
    public static class Measures {
        private String metric;
        private double value;
    }
}
