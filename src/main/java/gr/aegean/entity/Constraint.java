package gr.aegean.entity;

import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Constraint {
    private Integer analysisId;
    private QualityMetric qualityMetric;
    private QualityMetricOperator operator;
    private Double threshold;

    public boolean matchOperatorToCondition(Double metricValue) {
        return switch (operator) {
            case GT -> metricValue > threshold;
            case GTE -> metricValue >= threshold;
            case LT -> metricValue < threshold;
            case LTE -> metricValue <= threshold;
            case EQ -> metricValue.equals(threshold);
            case NEQ -> !metricValue.equals(threshold);
        };
    }
}
