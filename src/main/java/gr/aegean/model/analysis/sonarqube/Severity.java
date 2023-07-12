package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;

@Getter
public enum Severity {
    BLOCKER(20.0),
    CRITICAL(40.0),
    MAJOR(60.0),
    MINOR(80.0),
    INFO(99.0);

    private final double value;

    Severity(double value) {
        this.value = value;
    }
}