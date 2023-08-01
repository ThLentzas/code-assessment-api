package gr.aegean.entity;

import gr.aegean.model.analysis.quality.QualityAttribute;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
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
