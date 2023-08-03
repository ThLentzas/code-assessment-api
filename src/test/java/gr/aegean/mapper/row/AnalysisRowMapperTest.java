package gr.aegean.mapper.row;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gr.aegean.entity.Analysis;


class AnalysisRowMapperTest {
    private AnalysisRowMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new AnalysisRowMapper();
    }

    @Test
    void shouldMapRowToAnalysis() throws SQLException {
        try (ResultSet resultSet = mock(ResultSet.class)) {
            LocalDate createdDate = LocalDate.now();
            Analysis expected = new Analysis(1, 5, createdDate);

            when(resultSet.getInt("id")).thenReturn(1);
            when(resultSet.getInt("user_id")).thenReturn(5);
            when(resultSet.getDate("created_date")).thenReturn(Date.valueOf(createdDate));

            Analysis actual = underTest.mapRow(resultSet, 1);

            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getUserId()).isEqualTo(expected.getUserId());
            assertThat(actual.getCreatedDate()).isEqualTo(expected.getCreatedDate());
        }
    }
}

