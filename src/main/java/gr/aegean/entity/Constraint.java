package gr.aegean.entity;

import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
