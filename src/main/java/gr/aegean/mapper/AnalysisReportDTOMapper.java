package gr.aegean.mapper;

import gr.aegean.entity.AnalysisReport;
import gr.aegean.model.analysis.AnalysisReportDTO;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.function.Function;

@Service
public class AnalysisReportDTOMapper implements Function<AnalysisReport, AnalysisReportDTO> {
    @Override
    public AnalysisReportDTO apply(AnalysisReport report) {
        return new AnalysisReportDTO(
                report.getId(),
                report.getAnalysisId(),
                report.getLanguages(),
                report.getIssuesReport(),
                report.getHotspotsReport(),
                report.getRuleDetails(),
                report.getQualityMetricDetails(),
                Link.of(getBaseUrl() + "/analysis" + "/report" + "/" + report.getId())
        );
    }

    private String getBaseUrl() {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .build()
                .toUriString() + "/api/v1";
    }
}
