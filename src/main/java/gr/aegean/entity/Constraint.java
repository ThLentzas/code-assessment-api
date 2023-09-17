package gr.aegean.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;


@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Constraint {
    private Integer analysisId;
    @NotNull(message = "Quality metric is required")
    private QualityMetric qualityMetric;
    @NotNull(message = "Quality metric operator is required")
    private QualityMetricOperator qualityMetricOperator;
    @NotNull(message = "Threshold is required")
    @DecimalMin(value = "0.0", message = "Threshold value must be in the range of [0.0 - 1.0]")
    @DecimalMax(value = "1.0", message = "Threshold value must be in the range of [0.0 - 1.0]")
    private Double threshold;

    public Constraint(@NotNull(message = "Quality metric is required")
                      QualityMetric qualityMetric,
                      @NotNull(message = "Quality metric operator is required")
                      QualityMetricOperator operator,
                      @NotNull(message = "Threshold is required")
                      @DecimalMin(value = "0.0", message = "Threshold value must be in the range of [0.0 - 1.0]")
                      @DecimalMax(value = "1.0", message = "Threshold value must be in the range of [0.0 - 1.0]")
                      Double threshold) {
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

