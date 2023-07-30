package gr.aegean.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.aegean.entity.Analysis;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.mapper.row.AnalysisReportRowMapper;
import gr.aegean.mapper.row.AnalysisRowMapper;
import gr.aegean.mapper.row.ConstraintRowMapper;
import gr.aegean.mapper.row.PreferenceRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AnalysisRepository {
    private final JdbcTemplate jdbcTemplate;
    private final AnalysisRowMapper analysisRowMapper;
    private final AnalysisReportRowMapper reportRowMapper;
    private final PreferenceRowMapper preferenceRowMapper;
    private final ConstraintRowMapper constraintRowMapper;
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";

    public Integer saveAnalysis(Analysis analysis) {
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

        return analysis.getId();
    }

    public void saveAnalysisReport(AnalysisReport report) {
        final String sql = "INSERT INTO analysis_report(analysis_id, report) VALUES(?, ?::jsonb)";
        String jsonReport;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            ObjectMapper mapper = new ObjectMapper();
            jsonReport = mapper.writeValueAsString(report);
        } catch (JsonProcessingException jpe) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }

        int insert = jdbcTemplate.update(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, report.getAnalysisId());
            preparedStatement.setString(2, jsonReport);

            return preparedStatement;
        }, keyHolder);

        if (insert == 1) {
            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null && keys.containsKey("id")) {
                report.setId((Integer) keys.get("id"));
            }
        }
    }

    public void saveAnalysisConstraint(Constraint constraint) {
        final String sql = "INSERT INTO analysis_constraint(" +
                "analysis_id, " +
                "quality_metric, " +
                "operator, " +
                "threshold) VALUES(?, CAST(? AS quality_metric), CAST(? AS operator), ?)";

        int insert = jdbcTemplate.update(
                sql,
                constraint.getAnalysisId(),
                constraint.getQualityMetric().name(),
                constraint.getOperator().name(),
                constraint.getThreshold());

        if (insert != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void saveAnalysisPreference(Preference preference) {
        final String sql = "INSERT INTO analysis_preference(" +
                "analysis_id, " +
                "quality_attribute, " +
                "weight) VALUES(?, CAST(? AS quality_attribute), ?)";

        int insert = jdbcTemplate.update(
                sql,
                preference.getAnalysisId(),
                preference.getQualityAttribute().name(),
                preference.getWeight());

        if (insert != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public Optional<List<AnalysisReport>> findAnalysisReportsByAnalysisId(Integer analysisId) {
        final String sql = "SELECT id, report FROM analysis_report WHERE analysis_id = ?";
        List<AnalysisReport> reports = jdbcTemplate.query(sql, reportRowMapper, analysisId);

        return Optional.of(reports);
    }

    public Optional<AnalysisReport> findAnalysisReportByReportId(Integer reportId) {
        final String sql = "SELECT id, report FROM analysis_report WHERE id = ?";
        List<AnalysisReport> reports = jdbcTemplate.query(sql, reportRowMapper, reportId);

        return reports.stream().findFirst();
    }

    /*
        Returns an empty list if not found. We don't have to return an optional here. An empty list means no preferences
         were provided.
     */
    public Optional<List<Preference>> findAnalysisPreferencesByAnalysisId(Integer analysisId) {
        final String sql = "SELECT " +
                "analysis_id, " +
                "quality_attribute, " +
                "weight FROM analysis_preference WHERE analysis_id = ?";
        List<Preference> preferences = jdbcTemplate.query(sql, preferenceRowMapper, analysisId);

        return Optional.of(preferences);
    }

    /*
        Returns an empty list if not found. We don't have to return an optional here. An empty list means no constraints
         were provided.
     */
    public Optional<List<Constraint>> findAnalysisConstraintsByAnalysisId(Integer analysisId) {
        final String sql = "SELECT " +
                "analysis_id, " +
                "quality_metric, " +
                "operator, " +
                "threshold FROM analysis_constraint WHERE analysis_id = ?";
        List<Constraint> constraints = jdbcTemplate.query(sql, constraintRowMapper, analysisId);

        return Optional.of(constraints);
    }

    public Optional<List<Analysis>> findAnalysesByUserId(Integer userId) {
        final String sql = "SELECT id, user_id, created_date FROM analysis WHERE user_id = ? ORDER BY created_date DESC";
        List<Analysis> analyses = jdbcTemplate.query(sql, analysisRowMapper, userId);

        return Optional.of(analyses);
    }

    public Optional<Analysis> findAnalysisByAnalysisId(Integer analysisId) {
        final String sql = "SELECT id, user_id, created_date FROM analysis WHERE id = ?";
        List<Analysis> analyses = jdbcTemplate.query(sql, analysisRowMapper, analysisId);

        return analyses.stream().findFirst();
    }
}