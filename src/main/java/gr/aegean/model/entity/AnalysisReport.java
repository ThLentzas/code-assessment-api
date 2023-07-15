package gr.aegean.model.entity;

import gr.aegean.model.analysis.sonarqube.HotspotsReport;
import gr.aegean.model.analysis.sonarqube.IssuesReport;
import gr.aegean.model.analysis.sonarqube.MetricReport;
import gr.aegean.model.analysis.sonarqube.Rule;

import java.util.List;
import java.util.Map;

public record AnalysisReport(
        IssuesReport issuesReport,
        HotspotsReport hotspotsReport,
        Map<String, Rule> ruleDetails,
        List<MetricReport.MetricDetails> metricDetails
) {
}
