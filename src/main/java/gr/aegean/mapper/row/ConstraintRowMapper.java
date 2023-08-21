package gr.aegean.mapper.row;

import gr.aegean.entity.Constraint;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;


public class ConstraintRowMapper implements RowMapper<Constraint> {
    @Override
    public Constraint mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Constraint(
                resultSet.getInt("analysis_id"),
                QualityMetric.valueOf(resultSet.getString("quality_metric")),
                QualityMetricOperator.valueOf(resultSet.getString("operator")),
                resultSet.getDouble("threshold"));
    }
}
