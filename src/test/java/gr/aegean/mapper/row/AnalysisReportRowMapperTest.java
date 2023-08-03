package gr.aegean.mapper.row;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gr.aegean.entity.AnalysisReport;


class AnalysisReportRowMapperTest {
    private AnalysisReportRowMapper underTest;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        underTest = new AnalysisReportRowMapper();
    }

    @Test
    void shouldMapRowToAnalysisReport() throws SQLException, JsonProcessingException {
        try(ResultSet resultSet = mock(ResultSet.class)) {
            //Arrange
            Map<String, Double> languages = new HashMap<>();
            AnalysisReport expected = new AnalysisReport();
            expected.setId(1);
            expected.setAnalysisId(3);
            expected.setLanguages(languages);

            String jsonReport = objectMapper.writeValueAsString(expected);

            when(resultSet.getInt("id")).thenReturn(1);
            when(resultSet.getString("report")).thenReturn(jsonReport);

            //Act
            AnalysisReport actual = underTest.mapRow(resultSet, 1);

            //Assert
            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getAnalysisId()).isEqualTo(expected.getAnalysisId());
            assertThat(actual.getLanguages()).isEqualTo(expected.getLanguages());
        }
    }
}
