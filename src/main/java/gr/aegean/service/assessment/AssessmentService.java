package gr.aegean.service.assessment;

import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AssessmentService {
    private final RankingService rankingService;
    private final FilteringService filteringService;

    public List<List<AnalysisReport>> assessAnalysisResult(List<AnalysisReport> reports,
                                                           List<Constraint> constraints,
                                                           List<Preference> preferences) {
        /*
            No constraints -> no filtering
         */
        if (constraints.isEmpty()) {
            List<AnalysisReport> rankedReports = new ArrayList<>(reports);
            for (AnalysisReport report : rankedReports) {
                report.setRank(rankingService.rankTree(report.getQualityMetricsReport(), preferences));
            }

            rankedReports.sort(Comparator.comparing(AnalysisReport::getRank).reversed());

            return List.of(rankedReports);
        }

        List<List<AnalysisReport>> filteredReportsList = filteringService.filter(reports, constraints);
        List<List<AnalysisReport>> rankedFilteredReports = new ArrayList<>();

        for (List<AnalysisReport> filteredReports : filteredReportsList) {
            for (AnalysisReport filteredReport : filteredReports) {
                filteredReport.setRank(rankingService.rankTree(filteredReport.getQualityMetricsReport(), preferences));
            }

            filteredReports.sort(Comparator.comparing(AnalysisReport::getRank).reversed());
            rankedFilteredReports.add(filteredReports);
        }

        return rankedFilteredReports;
    }
}
