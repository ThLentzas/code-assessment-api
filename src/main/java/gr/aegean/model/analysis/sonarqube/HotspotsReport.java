package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;

import java.util.List;

@Getter
public class HotspotsReport {
    private List<HotspotDetails> hotspots;

    @Getter
    public static class HotspotDetails {
        private String component;
        private String securityCategory;
        private VulnerabilityProbability vulnerabilityProbability;
        private Integer line;
        private String message;
        private String ruleKey;
    }
}
