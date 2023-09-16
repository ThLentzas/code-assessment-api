package gr.aegean.model.analysis.quality;


public enum QualityAttribute {
    QUALITY("Quality"),
    COMPREHENSION("Comprehension"),
    SIMPLICITY("Simplicity"),
    MAINTAINABILITY("Maintainability"),
    RELIABILITY("Reliability"),
    COMPLEXITY("Complexity"),
    SECURITY("Security"),
    COMMENT_RATE("Comment Rate"),
    METHOD_SIZE("Method Size"),
    DUPLICATION("Duplication"),
    BUG_SEVERITY("Bug Severity"),
    TECHNICAL_DEBT_RATIO("Technical Debt Ratio"),
    RELIABILITY_REMEDIATION_EFFORT("Reliability Remediation Effort"),
    CYCLOMATIC_COMPLEXITY("Cyclomatic Complexity"),
    COGNITIVE_COMPLEXITY("Cognitive Complexity"),
    VULNERABILITY_SEVERITY("Vulnerability Severity"),
    HOTSPOT_PRIORITY("Hotspot Priority"),
    SECURITY_REMEDIATION_EFFORT("Security Remediation Effort");

    private final String display;

    QualityAttribute(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }
}
