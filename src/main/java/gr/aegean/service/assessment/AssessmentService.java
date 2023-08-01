package gr.aegean.service.assessment;

import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


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
            List<AnalysisReport> rankedReports = reports.stream()
                    //Map is an alternative.
                    .peek(report ->
                            report.setRank(rankingService.rankTree(report.getQualityMetricsReport(), preferences)))
                    //Descending order based on the rank.
                    .sorted(Comparator.comparing(AnalysisReport::getRank).reversed())
                    .toList();

            return List.of(rankedReports);
        }

        List<List<AnalysisReport>> filteredReportsList = filteringService.filter(reports, constraints);
        List<List<AnalysisReport>> rankedFilteredReports = new ArrayList<>();

        for (List<AnalysisReport> filteredReports : filteredReportsList) {
            filteredReports = filteredReports.stream()
                    .peek(filteredReport ->
                            filteredReport.setRank(rankingService.rankTree(
                                    filteredReport.getQualityMetricsReport(), preferences)))
                    .sorted(Comparator.comparing(AnalysisReport::getRank).reversed())
                    .toList();

            rankedFilteredReports.add(filteredReports);
        }

        return rankedFilteredReports;
    }
}
