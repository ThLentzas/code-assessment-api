package gr.aegean.model.analysis;

import gr.aegean.model.analysis.sonarqube.HotspotsReport;
import gr.aegean.model.analysis.sonarqube.IssuesReport;
import gr.aegean.model.analysis.sonarqube.Rule;
import org.springframework.hateoas.Link;

import java.util.EnumMap;
import java.util.Map;

public record AnalysisReportDTO(
        Integer reportId,
        Integer analysisId,
        Map<String, Double> languages,
        IssuesReport issuesReport,
        HotspotsReport hotspotsReport,
        Map<String, Rule> ruleDetails,
        EnumMap<QualityMetric, Double> qualityMetricDetails,
        Link self
) {
}
