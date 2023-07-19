package gr.aegean.service;

import gr.aegean.entity.AnalysisReport;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class RankingService {

    public List<List<AnalysisReport>> rankProjects(List<AnalysisReport> reports) {
//        List<AnalysisReport> compliantReports = new ArrayList<>();
//        List<AnalysisReport> nonCompliantReports = new ArrayList<>();
//
//        for(Constraint constraint : constraints) {
//            for(AnalysisReport report : reports) {
//                Double metricValue = report.qualityMetricDetails().get(constraint.getQualityMetric());
//
//                if(constraint.matches(metricValue)) {
//                    compliantReports.add(report);
//                } else {
//                    nonCompliantReports.add(report);
//                }
//            }
//        }

        return null;
    }
}
