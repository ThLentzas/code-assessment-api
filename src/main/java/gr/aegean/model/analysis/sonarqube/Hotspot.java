package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;

import java.util.List;

@Getter
public class Hotspot {
    private List<HotspotDetails> hotspots;

    @Getter
    public static class HotspotDetails {
        private String key;
        private String component;
        private String securityCategory;
        private String vulnerabilityProbability;
        private Integer line;
        private String message;
        private String ruleKey;
    }
}
