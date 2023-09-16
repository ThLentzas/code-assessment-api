package gr.aegean.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;


@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Constraint {
    private Integer analysisId;
    private QualityMetric qualityMetric;
    private QualityMetricOperator qualityMetricOperator;
    private Double threshold;

    public Constraint(QualityMetric qualityMetric, QualityMetricOperator operator, Double threshold) {
        this.qualityMetric = qualityMetric;
        this.qualityMetricOperator = operator;
        this.threshold = threshold;
    }

    public boolean matchOperatorToCondition(Double metricValue) {
        return switch (qualityMetricOperator) {
            case GT -> metricValue > threshold;
            case GTE -> metricValue >= threshold;
            case LT -> metricValue < threshold;
            case LTE -> metricValue <= threshold;
            case EQ -> metricValue.equals(threshold);
            case NEQ -> !metricValue.equals(threshold);
        };
    }
}

