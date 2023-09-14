package gr.aegean.entity;

import lombok.*;

import gr.aegean.model.analysis.quality.QualityAttribute;


@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Preference {
    private Integer analysisId;
    private QualityAttribute qualityAttribute;
    private Double weight;

    public Preference(QualityAttribute qualityAttribute, Double weight) {
        this.qualityAttribute = qualityAttribute;
        this.weight = weight;
    }
}
