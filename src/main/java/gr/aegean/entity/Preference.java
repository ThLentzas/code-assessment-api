package gr.aegean.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import gr.aegean.model.analysis.quality.QualityAttribute;


@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Preference {
    private Integer analysisId;
    @NotNull(message = "Quality attribute is required")
    private QualityAttribute qualityAttribute;
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.0", message = "Weight value must be in the range of [0.0 - 1.0]")
    @DecimalMax(value = "1.0", message = "Weight value must be in the range of [0.0 - 1.0]")
    private Double weight;

    public Preference(@NotNull(message = "Quality attribute is required")
                      QualityAttribute qualityAttribute,
                      @NotNull(message = "Weigh is required")
                      @DecimalMin(value = "0.0", message = "Weight value must be in the range of [0.0 - 1.0]")
                      @DecimalMax(value = "1.0", message = "Weight value must be in the range of [0.0 - 1.0]")
                      Double weight) {
        this.qualityAttribute = qualityAttribute;
        this.weight = weight;
    }
}
