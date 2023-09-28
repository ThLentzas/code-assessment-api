package gr.aegean.mapper.dto;

import gr.aegean.entity.AnalysisReport;
import gr.aegean.model.dto.analysis.AnalysisReportDTO;

import java.util.function.Function;


public class AnalysisReportDTOMapper implements Function<AnalysisReport, AnalysisReportDTO> {
    @Override
    public AnalysisReportDTO apply(AnalysisReport report) {
        return new AnalysisReportDTO(
                report.getId(),
                report.getAnalysisId(),
                report.getProjectUrl(),
                report.getLanguages(),
                report.getIssuesReport(),
                report.getHotspotsReport(),
                report.getQualityMetricsReport(),
                report.getRank());
    }
}
