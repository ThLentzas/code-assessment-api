package gr.aegean.model.analysis;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.List;

/*
    A list containing the 2 ranked lists of analysis reports.
 */
public record AnalysisResult(Integer analysisId,
                             List<List<AnalysisReportDTO>> reports,
                             @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                             @JsonSerialize(using = LocalDateTimeSerializer.class)
                             LocalDateTime createdDate) {
}
