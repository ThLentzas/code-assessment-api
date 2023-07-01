package gr.aegean.mapper;

import gr.aegean.model.token.EmailUpdateToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailUpdateTokenRowMapperTest {

    private EmailUpdateTokenRowMapper rowMapper;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        rowMapper = new EmailUpdateTokenRowMapper();
        resultSet = mock(ResultSet.class);
    }

    @Test
    void shouldMapRowIntoEmailUpdateToken() throws SQLException {
        // Arrange
        LocalDateTime expiryDate = LocalDateTime.now();
        when(resultSet.getInt("user_id")).thenReturn(1);
        when(resultSet.getString("token")).thenReturn("token");
        when(resultSet.getString("email")).thenReturn("email");
        when(resultSet.getTimestamp("expiry_date")).thenReturn(Timestamp.valueOf(expiryDate));

        // Act
        EmailUpdateToken emailUpdateToken = rowMapper.mapRow(resultSet, 1);

        // Assert
        assertThat(1).isEqualTo(emailUpdateToken.userId());
        assertThat("token").isEqualTo(emailUpdateToken.token());
        assertThat("email").isEqualTo(emailUpdateToken.email());
        assertThat(expiryDate).isEqualTo(emailUpdateToken.expiryDate());
    }
}