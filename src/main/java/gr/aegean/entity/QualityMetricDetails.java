package gr.aegean.entity;

import gr.aegean.model.analysis.QualityMetric;
import gr.aegean.model.analysis.QualityMetricOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class QualityMetricDetails {
    private QualityMetric qualityMetric;
    private QualityMetricOperator operator;
    private Double threshold;
    private Double weight;

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
