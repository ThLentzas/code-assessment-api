package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;

import java.util.List;

@Getter
public class Rule {
    private RuleDetails rule;

    @Getter
    public static class RuleDetails {
        private String key;
        private String name;
        private Severity severity;
        private List<DescriptionSection> descriptionSections;


        @Getter
        public static class DescriptionSection {
            private String key;
            private String content;

        }
    }
}
