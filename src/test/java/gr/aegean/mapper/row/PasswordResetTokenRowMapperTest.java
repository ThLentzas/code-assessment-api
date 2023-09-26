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

    /*
        We cannot create a PasswordResetToken object and then use assertThat(actual).isEqualTo(expected) because of the
        expiry date property. For example: PasswordResetToken expected = new PasswordResetToken(
        1, "token", LocalDate.now()) and then call when(resultSet.getTimestamp("expiry_date")).thenReturn(
        Timestamp.valueOf(expiryDate)); some time will have elapsed between the creation of the expected token and
        the actual mapping from the resultSet, making the two expiryDates different. Therefore, calling equals and
        hashcode will treat them as two different objects.
     */

    @Test
    void shouldMapRowToPasswordResetToken() throws SQLException {
        try (ResultSet resultSet = mock(ResultSet.class)) {
            // Arrange
            LocalDateTime expiryDate = LocalDateTime.now();

            when(resultSet.getInt("user_id")).thenReturn(1);
            when(resultSet.getString("token")).thenReturn("token");
            when(resultSet.getTimestamp("expiry_date")).thenReturn(Timestamp.valueOf(expiryDate));

            // Act
            PasswordResetToken actual = underTest.mapRow(resultSet, 1);

            // Assert
            assertThat(actual.userId()).isEqualTo(1);
            assertThat(actual.token()).isEqualTo("token");
            assertThat(actual.expiryDate()).isEqualTo(expiryDate);
        }
    }
}