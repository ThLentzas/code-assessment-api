package gr.aegean.mapper.row;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import gr.aegean.entity.Preference;
import gr.aegean.model.analysis.quality.QualityAttribute;


class PreferenceRowMapperTest {
    private PreferenceRowMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new PreferenceRowMapper();
    }

    @Test
    void shouldMapRowToPreference() throws SQLException {
        try (ResultSet resultSet = mock(ResultSet.class)) {
            // Arrange
            Preference expected = new Preference(1, QualityAttribute.BUG_SEVERITY, 0.4);

            when(resultSet.getInt("analysis_id")).thenReturn(1);
            when(resultSet.getString("quality_attribute")).thenReturn(QualityAttribute.BUG_SEVERITY.name());
            when(resultSet.getDouble("weight")).thenReturn(0.4);

            //Act
            Preference actual = underTest.mapRow(resultSet, 1);

            //Assert
            assertThat(actual.getAnalysisId()).isEqualTo(expected.getAnalysisId());
            assertThat(actual.getQualityAttribute()).isEqualTo(expected.getQualityAttribute());
            assertThat(actual.getWeight()).isEqualTo(expected.getWeight());
        }
    }
}
