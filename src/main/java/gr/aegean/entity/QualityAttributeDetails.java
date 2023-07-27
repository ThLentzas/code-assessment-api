package gr.aegean.entity;

import gr.aegean.model.analysis.quality.QualityAttribute;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class QualityAttributeDetails {
    private Integer analysisId;
    private QualityAttribute qualityAttribute;
    private Double weight;
}
