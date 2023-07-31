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

import gr.aegean.entity.PasswordResetToken;


class PasswordResetTokenRowMapperTest {
    private PasswordResetTokenRowMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new PasswordResetTokenRowMapper();

    }

    @Test
    void shouldMapRowToPasswordResetToken() throws SQLException {
        // Arrange
        try (ResultSet resultSet = mock(ResultSet.class)) {
            LocalDateTime expiryDate = LocalDateTime.now();
            when(resultSet.getInt("user_id")).thenReturn(1);
            when(resultSet.getString("token")).thenReturn("token");
            when(resultSet.getTimestamp("expiry_date")).thenReturn(Timestamp.valueOf(expiryDate));

            // Act
            PasswordResetToken passwordResetToken = underTest.mapRow(resultSet, 1);

            // Assert
            assertThat(1).isEqualTo(passwordResetToken.userId());
            assertThat("token").isEqualTo(passwordResetToken.token());
            assertThat(expiryDate).isEqualTo(passwordResetToken.expiryDate());
        }
    }
}