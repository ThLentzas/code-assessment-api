package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class Rule {
    private RuleDetails rule;

    @Getter
    @Setter
    public static class RuleDetails {
        private String key;
        private String name;
        private Severity severity;
        private List<DescriptionSection> descriptionSections;

        @Getter
        @Setter
        public static class DescriptionSection {
            private String key;
            private String content;

        }
    }
}
