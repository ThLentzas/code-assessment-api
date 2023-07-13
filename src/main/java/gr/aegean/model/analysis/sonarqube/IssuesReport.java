package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;

import java.util.List;


/*
    For the mapping we need getters and a default constructor, the object mapper will use them to map the response from
    the server to our SonarResponse POJO. This class will map BUGS,CODE SMELLS AND VULNERABILITIES. HOTSPOTS is a
    separate request.
 */

@Getter
public class IssuesReport {
    private List<IssueDetails> issues;
    private Paging paging;

    @Getter
    public static class IssueDetails {
        private String key;
        private String rule;
        private Severity severity;
        private String component;
        private String project;
        private int line;
        private String message;
        private String type;
        private List<Flow> flows;
        private TextRange textRange;

        @Getter
        public static class TextRange {
            private int startLine;
            private int endLine;
            private int startOffset;
            private int endOffset;
        }

        @Getter
        public static class Flow {
            private List<Location> locations;

            @Getter
            public static class Location {
                private TextRange textRange;
                private String msg;

            }
        }
    }
}

