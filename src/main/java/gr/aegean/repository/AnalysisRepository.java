package gr.aegean.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.aegean.entity.Analysis;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.QualityMetricDetails;
import gr.aegean.exception.ServerErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AnalysisRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";

    public Analysis saveAnalysis(Analysis analysis) {
        final String sql = "INSERT INTO analysis(user_id, created_date) VALUES(?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int insert = jdbcTemplate.update(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, analysis.getUserId());
            preparedStatement.setTimestamp(2, Timestamp.valueOf(analysis.getCreatedDate()));

            return preparedStatement;
        }, keyHolder);

        if (insert == 1) {
            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null && keys.containsKey("id")) {
                analysis.setId((Integer) keys.get("id"));
            }
        }

        return analysis;
    }

    public void saveAnalysisReport(AnalysisReport report) {
        final String sql = "INSERT INTO analysis_report(analysis_id, report) VALUES(?, ?::jsonb)";
        String jsonReport;

        try {
            ObjectMapper mapper = new ObjectMapper();
            jsonReport = mapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }

        int insert = jdbcTemplate.update(sql, report.getAnalysisId(), jsonReport);
        if (insert != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void saveQualityMetricDetails(Integer analysisId, QualityMetricDetails metricDetails) {
        final String sql = "INSERT INTO quality_metric_details(" +
                "analysis_id, " +
                "quality_metric, " +
                "operator, " +
                "threshold, " +
                "weight) VALUES(?, CAST(? AS quality_metric), CAST(? AS operator), ?, ?)";

        int insert = jdbcTemplate.update(
                sql,
                analysisId,
                metricDetails.getQualityMetric().name(),
                metricDetails.getOperator().getSymbol(),
                metricDetails.getThreshold(),
                metricDetails.getWeight());
        if (insert != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }
}
