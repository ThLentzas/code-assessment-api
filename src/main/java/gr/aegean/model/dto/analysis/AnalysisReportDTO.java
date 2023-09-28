package gr.aegean.model.dto.analysis;

import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.sonarqube.HotspotsReport;
import gr.aegean.model.analysis.sonarqube.IssuesReport;

import java.util.Map;

import org.springframework.hateoas.Link;


public record AnalysisReportDTO(
        Integer reportId,
        Integer analysisId,
        Link projectUrl,
        Map<String, Double> languages,
        IssuesReport issuesReport,
        HotspotsReport hotspotsReport,
        Map<QualityMetric, Double> qualityMetricsReport,
        Double rank) {
}
