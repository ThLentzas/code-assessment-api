package gr.aegean.repository;

import gr.aegean.entity.Analysis;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.entity.Constraint;
import gr.aegean.entity.Preference;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.mapper.row.AnalysisReportRowMapper;
import gr.aegean.mapper.row.AnalysisRowMapper;
import gr.aegean.mapper.row.ConstraintRowMapper;
import gr.aegean.mapper.row.PreferenceRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;


@Repository
@RequiredArgsConstructor
public class AnalysisRepository {
    private final JdbcTemplate jdbcTemplate;
    private final AnalysisRowMapper analysisRowMapper = new AnalysisRowMapper();
    private final AnalysisReportRowMapper reportRowMapper = new AnalysisReportRowMapper();
    private final PreferenceRowMapper preferenceRowMapper = new PreferenceRowMapper();
    private final ConstraintRowMapper constraintRowMapper = new ConstraintRowMapper();
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";

    public Integer saveAnalysis(Analysis analysis) {
        final String sql = "INSERT INTO analysis(user_id, created_date) VALUES(?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int insert = jdbcTemplate.update(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, analysis.getUserId());
            preparedStatement.setDate(2, Date.valueOf(analysis.getCreatedDate()));

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

        jdbcTemplate.update(
                sql,
                constraint.getAnalysisId(),
                constraint.getQualityMetric().name(),
                constraint.getQualityMetricOperator().name(),
                constraint.getThreshold());
    }

    public void saveAnalysisPreference(Preference preference) {
        final String sql = "INSERT INTO analysis_preference(" +
                "analysis_id, " +
                "quality_attribute, " +
                "weight) VALUES(?, CAST(? AS quality_attribute), ?)";

        jdbcTemplate.update(
                sql,
                preference.getAnalysisId(),
                preference.getQualityAttribute().name(),
                preference.getWeight());
    }

    /*
        queryForObject() will throw EmptyResultDataAccessException when the query is expected to return a single row,
        but no rows are returned and IncorrectResultSizeDataAccessException when more than one row is returned. It will
        the result object of the required type, or null in case of SQL NULL. By using Optional.ofNullable() we ensure an
        empty optional in case queryForObject() returns null
        EmptyResultDataAccessException extends IncorrectResultSizeDataAccessException so by catching the parent class
        we deal with both cases
     */
    public Optional<Analysis> findAnalysisByAnalysisId(Integer analysisId) {
        final String sql = "SELECT id, user_id, created_date FROM analysis WHERE id = ?";

        try {
            Analysis analysis = jdbcTemplate.queryForObject(sql, analysisRowMapper, analysisId);

            return Optional.ofNullable(analysis);
        } catch (IncorrectResultSizeDataAccessException ire) {
            return Optional.empty();
        }
    }

    /*
       The reason why this method does not return an optional is because, no analysis should have been saved if an empty
       list of repositories was provided.
     */
    public List<AnalysisReport> findAnalysisReportsByAnalysisId(Integer analysisId) {
        final String sql = "SELECT id, report FROM analysis_report WHERE analysis_id = ?";

        return (jdbcTemplate.query(sql, reportRowMapper, analysisId));
    }

    public Optional<List<Preference>> findAnalysisPreferencesByAnalysisId(Integer analysisId) {
        final String sql = "SELECT " +
                "analysis_id, " +
                "quality_attribute, " +
                "weight FROM analysis_preference WHERE analysis_id = ?";

        List<Preference> preferences = jdbcTemplate.query(sql, preferenceRowMapper, analysisId);

        return Optional.of(preferences);
    }

    public Optional<List<Constraint>> findAnalysisConstraintsByAnalysisId(Integer analysisId) {
        final String sql = "SELECT " +
                "analysis_id, " +
                "quality_metric, " +
                "operator, " +
                "threshold FROM analysis_constraint WHERE analysis_id = ?";

        List<Constraint> constraints = jdbcTemplate.query(sql, constraintRowMapper, analysisId);

        return Optional.of(constraints);
    }

    /*
       If there are multiple rows with the same created_date, the one with the higher id will appear first in the
       result set.
     */
    public Optional<List<Analysis>> getHistory(Integer userId) {
        final String sql = "SELECT " +
                "id, " +
                "user_id, " +
                "created_date FROM analysis WHERE user_id = ? ORDER BY created_date DESC, id DESC";

        List<Analysis> analyses = jdbcTemplate.query(sql, analysisRowMapper, userId);

        return Optional.of(analyses);
    }

    public Optional<List<Analysis>> getHistoryInDateRange(Integer userId, Date from, Date to) {
        final String sql = "SELECT " +
                "id, " +
                "user_id, " +
                "created_date FROM analysis WHERE user_id = ? AND " +
                "created_date BETWEEN ? AND ? ORDER BY created_date DESC";

        List<Analysis> analyses = jdbcTemplate.query(sql, analysisRowMapper, userId, from, to);

        return Optional.of(analyses);
    }

    public void deleteAnalysis(Integer analysisId, Integer userId) {
        final String sql = "DELETE FROM analysis WHERE id = ? AND user_id = ?";

        jdbcTemplate.update(sql, analysisId, userId);
    }

    public void deleteAllAnalyses() {
        final String sql = "DELETE FROM analysis";

        jdbcTemplate.update(sql);
    }

    public void deleteConstraintByAnalysisId(Integer analysisId) {
        final String sql = "DELETE FROM analysis_constraint WHERE analysis_id = ?";

        jdbcTemplate.update(sql, analysisId);
    }

    public void deletePreferenceByAnalysisId(Integer analysisId) {
        final String sql = "DELETE FROM analysis_preference WHERE analysis_id = ?";

        jdbcTemplate.update(sql, analysisId);
    }
}
