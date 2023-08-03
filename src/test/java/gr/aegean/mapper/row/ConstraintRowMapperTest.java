package gr.aegean.mapper.row;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gr.aegean.entity.Constraint;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;


class ConstraintRowMapperTest {
    private ConstraintRowMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new ConstraintRowMapper();
    }

    @Test
    void shouldMapRowToConstraint() throws SQLException {
        try (ResultSet resultSet = mock(ResultSet.class)) {
            //Arrange
            Constraint expected = new Constraint(1, QualityMetric.DUPLICATION, QualityMetricOperator.LT, 0.8);

            when(resultSet.getInt("analysis_id")).thenReturn(1);
            when(resultSet.getString("quality_metric")).thenReturn(QualityMetric.DUPLICATION.name());
            when(resultSet.getString("operator")).thenReturn(QualityMetricOperator.LT.name());
            when(resultSet.getDouble("threshold")).thenReturn(0.8);

            //Act
            Constraint actual = underTest.mapRow(resultSet, 1);

            //Assert
            assertThat(actual.getAnalysisId()).isEqualTo(expected.getAnalysisId());
            assertThat(actual.getQualityMetric()).isEqualTo(expected.getQualityMetric());
            assertThat(actual.getOperator()).isEqualTo(expected.getOperator());
            assertThat(actual.getThreshold()).isEqualTo(expected.getThreshold());
        }
    }
}
