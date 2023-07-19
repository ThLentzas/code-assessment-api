package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;


@Getter
public enum Severity {
    BLOCKER(0.2),
    CRITICAL(0.4),
    MAJOR(0.6),
    MINOR(0.8),
    INFO(0.99);

    private final double value;

    Severity(double value) {
        this.value = value;
    }
}