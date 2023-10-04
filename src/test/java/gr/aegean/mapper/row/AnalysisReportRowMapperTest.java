package gr.aegean.mapper.row;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.aegean.entity.AnalysisReport;


class AnalysisReportRowMapperTest {
    private AnalysisReportRowMapper underTest;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        underTest = new AnalysisReportRowMapper();
    }

    @Test
    void shouldMapRowToAnalysisReport() throws SQLException, IOException {
        try (ResultSet resultSet = mock(ResultSet.class)) {
            //Arrange
            String analysisReportPath = "src/test/resources/reports/analysis-reports.json";
            CollectionType type = objectMapper.getTypeFactory().constructCollectionType(
                    List.class,
                    AnalysisReport.class);
            List<AnalysisReport> reports = objectMapper.readValue(new File(analysisReportPath), type);
            AnalysisReport expected = reports.get(0);
            String jsonReport = objectMapper.writeValueAsString(expected);

            when(resultSet.getInt("id")).thenReturn(3);
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
