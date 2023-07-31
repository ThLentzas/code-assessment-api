package gr.aegean.mapper.row;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.aegean.entity.AnalysisReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            Map<String, Double> languages = new HashMap<>();
            AnalysisReport expected = new AnalysisReport();
            expected.setId(1);
            expected.setAnalysisId(3);
            expected.setLanguages(languages);

            String jsonReport = objectMapper.writeValueAsString(expected);

            when(resultSet.getInt("id")).thenReturn(1);
            when(resultSet.getString("report")).thenReturn(jsonReport);

            AnalysisReport actual = underTest.mapRow(resultSet, 1);

            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getAnalysisId()).isEqualTo(expected.getAnalysisId());
            assertThat(actual.getLanguages()).isEqualTo(expected.getLanguages());
        }
    }
}
