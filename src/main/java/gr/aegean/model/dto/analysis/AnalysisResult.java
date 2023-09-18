package gr.aegean.model.dto.analysis;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import java.time.LocalDate;
import java.util.List;

/*
    A list containing the 2 ranked lists of analysis reports.
 */
public record AnalysisResult(Integer analysisId,
                             List<List<AnalysisReportDTO>> reports,
                             @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                               @JsonSerialize(using = LocalDateSerializer.class)
                               LocalDate createdDate) {
}