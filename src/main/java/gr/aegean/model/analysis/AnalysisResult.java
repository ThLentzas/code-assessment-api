package gr.aegean.model.analysis;

import java.util.List;

/*
    A list containing the 2 ranked lists of analysis reports.
 */
public record AnalysisResult(List<List<AnalysisReportDTO>> reports) {
}
