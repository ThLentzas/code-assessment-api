package gr.aegean.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.sonarqube.HotspotsReport;
import gr.aegean.model.analysis.sonarqube.IssuesReport;

import java.util.Map;

import org.springframework.hateoas.Link;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisReport {
    private Integer id;
    private Integer analysisId;
    private Link projectUrl;
    private Map<String, Double> languages;
    private IssuesReport issuesReport;
    private HotspotsReport hotspotsReport;
    private Map<QualityMetric, Double> qualityMetricsReport;
    private Double rank;

    public AnalysisReport(IssuesReport issuesReport,
                          HotspotsReport hotspotsReport,
                          Map<QualityMetric, Double> qualityMetricsReport) {
        this.issuesReport = issuesReport;
        this.hotspotsReport = hotspotsReport;
        this.qualityMetricsReport = qualityMetricsReport;
    }
}

