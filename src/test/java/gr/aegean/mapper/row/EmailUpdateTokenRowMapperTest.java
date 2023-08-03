package gr.aegean.mapper.row;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gr.aegean.entity.EmailUpdateToken;


class EmailUpdateTokenRowMapperTest {
    private EmailUpdateTokenRowMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new EmailUpdateTokenRowMapper();
    }

    @Test
    void shouldMapRowToEmailUpdateToken() throws SQLException {
        try (ResultSet resultSet = mock(ResultSet.class)) {
            // Arrange
            LocalDateTime expiryDate = LocalDateTime.now();

            when(resultSet.getInt("user_id")).thenReturn(1);
            when(resultSet.getString("token")).thenReturn("token");
            when(resultSet.getString("email")).thenReturn("email");
            when(resultSet.getTimestamp("expiry_date")).thenReturn(Timestamp.valueOf(expiryDate));

            // Act
            EmailUpdateToken emailUpdateToken = underTest.mapRow(resultSet, 1);

            // Assert
            assertThat(emailUpdateToken.userId()).isEqualTo(1);
            assertThat(emailUpdateToken.token()).isEqualTo("token");
            assertThat(emailUpdateToken.email()).isEqualTo("email");
            assertThat(emailUpdateToken.expiryDate()).isEqualTo(expiryDate);
        }
    }
}