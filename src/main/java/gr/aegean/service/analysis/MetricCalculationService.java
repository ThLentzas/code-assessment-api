package gr.aegean.service.analysis;

import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.sonarqube.IssuesReport;
import gr.aegean.model.analysis.sonarqube.Severity;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
    For a Quality metric the relative utility function(utf) is applied.
 */
@Service
public class MetricCalculationService {
    private static final double LINE_COST = 0.06;

    public EnumMap<QualityMetric, Double> applyUtf(EnumMap<QualityMetric, Double> metricsDetails,
                                                   List<IssuesReport.IssueDetails> issueDetails) {
        EnumMap<QualityMetric, Double> updatedMetricsDetails = new EnumMap<>(QualityMetric.class);
        Double linesOfCode = metricsDetails.get(QualityMetric.LINES_OF_CODE);

        metricsDetails.forEach((metric, value) -> {
            switch (metric) {
                case METHOD_SIZE -> updatedMetricsDetails.put(metric, applyMethodSizeUtf(value));
                case DUPLICATION -> updatedMetricsDetails.put(metric, applyDuplicationUtf(value));
                case TECHNICAL_DEBT_RATIO -> updatedMetricsDetails.put(metric, applyTechnicalDebtRatioUtf(value));
                case RELIABILITY_REMEDIATION_EFFORT -> updatedMetricsDetails.put(
                        metric,
                        applyReliabilityRemediationEffortUtf(value, linesOfCode));
                case CYCLOMATIC_COMPLEXITY, COGNITIVE_COMPLEXITY -> updatedMetricsDetails.put(
                        metric,
                        applyComplexityUtf(value, linesOfCode));
                case SECURITY_REMEDIATION_EFFORT -> updatedMetricsDetails.put(
                        metric,
                        applySecurityRemediationEffortUtf(value, linesOfCode));
            }
        });

        // TODO: 7/28/2023 Add to the updated metric report the bug severity, vulnerability severity and the hotspot
        //  priority


        return updatedMetricsDetails;
    }

    private double applyDuplicationUtf(double duplication) {
        return 1 - duplication;
    }

    private double applyMethodSizeUtf(double methodSize) {
        return Math.pow(2, (70 - methodSize) / 21) / 3;
    }

    private double applyTechnicalDebtRatioUtf(double technicalDeptRatio) {
        return 1 - technicalDeptRatio;
    }

    private double applyReliabilityRemediationEffortUtf(double reliabilityRemediationEffort, double linesOfCode) {
        return 1 - reliabilityRemediationEffort / (linesOfCode * LINE_COST);
    }

    private double applyComplexityUtf(double complexity, double linesOfCode) {
        return  1 - complexity / linesOfCode;
    }

    private double applySecurityRemediationEffortUtf(double securityRemediationEffort, double linesOfCode) {
        return 1 - securityRemediationEffort / (linesOfCode * LINE_COST);
    }

    /*
        This method will be applied for bug severity and vulnerability severity metrics.
     */
    private double applyMetricsUtf(Map<Severity, Long> severityCount) {
        long blockerCount = severityCount.get(Severity.BLOCKER);
        long criticalCount = severityCount.get(Severity.CRITICAL);
        long majorCount = severityCount.get(Severity.MAJOR);
        long minorCount = severityCount.get(Severity.MINOR);
        long infoCount = severityCount.get(Severity.INFO);

        if(blockerCount > 0) {
          return 0.2 * 1 / (blockerCount * (1 +
                  Math.pow(10, -1) * utf(criticalCount) +
                  Math.pow(10, -2) * utf(majorCount) +
                  Math.pow(10, -3) * utf(minorCount) +
                  Math.pow(10, -4) * utf(infoCount)));
        }

        if(criticalCount > 0) {
            return 0.2 * 1 / (criticalCount * (1 +
                    Math.pow(10, -1) * utf(majorCount) +
                    Math.pow(10, -2) * utf(minorCount) +
                    Math.pow(10, -3) * utf(infoCount))) + 0.2;
        }

        if(majorCount > 0) {
            return 0.2 * 1 / (majorCount * (1 +
                    Math.pow(10, -1) * utf(minorCount) +
                    Math.pow(10, -2) * utf(infoCount))) + 0.4;
        }

        if(minorCount > 0) {
            return 0.2 * 1 / (minorCount * (1 +
                    Math.pow(10, -1) * utf(infoCount))) + 0.6;
        }

        if(infoCount > 0) {
            return 0.2 * 1 / infoCount + 0.8;
        }

        /*
            If no severities are found(no bugs/vulnerabilities) we have the max value.
         */
        return 1.0;
    }

    private Map<Severity, Long> countSeverityByIssueType(List<IssuesReport.IssueDetails> issueDetails,
                                                         String issueType) {
        /*
            Returns a list like [BLOCKER, BLOCKER, BLOCKER, CRITICAL, MAJOR,  MINOR, MINOR, INFO]
         */
        List<Severity> sorted = issueDetails.stream()
                .filter(issue -> issue.getType().equals(issueType))
                .map(IssuesReport.IssueDetails::getSeverity)
                .sorted()
                .toList();

        /*
            BLOCKER: 3,
            CRITICAL: 1,
            MAJOR: 1,
            MINOR: 2,
            INFO: 1

            We know the total occurrences of each severity, so we can apply the relative utf.
         */
        return sorted.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private double utf(long severityCount) {
        return severityCount == 0 ? 0 : 1.0 / (1.0 + 1.0 / (1.0 + severityCount));
    }
}
