package gr.aegean.mapper;

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

    private PasswordResetTokenRowMapper mapper;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        mapper = new PasswordResetTokenRowMapper();
        resultSet = mock(ResultSet.class);
    }

    @Test
    void shouldMapRowIntoPasswordResetToken() throws SQLException {
        // Arrange
        LocalDateTime expiryDate = LocalDateTime.now();
        when(resultSet.getInt("user_id")).thenReturn(1);
        when(resultSet.getString("token")).thenReturn("token");
        when(resultSet.getTimestamp("expiry_date")).thenReturn(Timestamp.valueOf(expiryDate));

        // Act
        PasswordResetToken passwordResetToken = mapper.mapRow(resultSet, 1);

        // Assert
        assertThat(1).isEqualTo(passwordResetToken.userId());
        assertThat("token").isEqualTo(passwordResetToken.token());
        assertThat(expiryDate).isEqualTo(passwordResetToken.expiryDate());
    }
}