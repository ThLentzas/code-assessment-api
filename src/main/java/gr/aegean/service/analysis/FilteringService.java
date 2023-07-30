package gr.aegean.service.analysis;

import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class FilteringService {
    public List<List<AnalysisReport>> filter(List<AnalysisReport> reports, List<Constraint> constraints) {
        /*
            If all reports are compliant or all reports are non-compliant, in each case an empty list will be returned.
         */
        List<AnalysisReport> compliant = new ArrayList<>();
        List<AnalysisReport> nonCompliant = new ArrayList<>();

        for(AnalysisReport report : reports) {
            boolean isCompliant = constraints.stream()
                    .allMatch(constraint ->
                            constraint.matchOperatorToCondition(
                                    report.getQualityMetricsReport().get(constraint.getQualityMetric())));

            if (isCompliant) {
                compliant.add(report);
            } else {
                nonCompliant.add(report);
            }
        }

        return List.of(compliant, nonCompliant);
    }
}
