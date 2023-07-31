package gr.aegean.mapper.row;

import gr.aegean.entity.Analysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class AnalysisRowMapperTest {
    private AnalysisRowMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new AnalysisRowMapper();
    }

    @Test
    void shouldMapRowToAnalysis() throws SQLException {
        try (ResultSet resultSet = mock(ResultSet.class)) {
            LocalDateTime createdDate = LocalDateTime.now();
            Analysis expected = new Analysis(1, 5, createdDate);

            when(resultSet.getInt("id")).thenReturn(1);
            when(resultSet.getInt("user_id")).thenReturn(5);
            when(resultSet.getTimestamp("created_date")).thenReturn(Timestamp.valueOf(createdDate));

            Analysis actual = underTest.mapRow(resultSet, 1);

            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getUserId()).isEqualTo(expected.getUserId());
            assertThat(actual.getCreatedDate()).isEqualTo(expected.getCreatedDate());
        }
    }
}

