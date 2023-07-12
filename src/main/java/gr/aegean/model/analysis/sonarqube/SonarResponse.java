package gr.aegean.model.analysis.sonarqube;

import lombok.Getter;

import java.util.List;

@Getter
public class SonarResponse {
    private int total;
    private int effortTotal;
    private List<Issue> issues;
}

