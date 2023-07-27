package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;


@Getter
public enum Severity {
    BLOCKER,
    CRITICAL,
    MAJOR,
    MINOR,
    INFO
}