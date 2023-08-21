package gr.aegean.mapper.row;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.exception.ServerErrorException;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;


public class AnalysisReportRowMapper implements RowMapper<AnalysisReport> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AnalysisReport mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        String jsonReport = resultSet.getString("report");
        AnalysisReport report;

        try {
            report = objectMapper.readValue(jsonReport, AnalysisReport.class);
            report.setId(resultSet.getInt("id"));
        } catch (JsonProcessingException jpe) {
            throw new ServerErrorException("The server encountered an internal error and was unable " + "to complete " +
                    "your request. Please try again later.");
        }

        return report;
    }
}

