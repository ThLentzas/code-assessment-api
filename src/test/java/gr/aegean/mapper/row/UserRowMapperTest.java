package gr.aegean.mapper.row;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import gr.aegean.entity.User;


class UserRowMapperTest {
    private UserRowMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new UserRowMapper();
    }

    @Test
    void shouldMapRowToUser() throws SQLException {
        try (ResultSet resultSet = mock(ResultSet.class)) {
            // Arrange
            User expected = User.builder()
                    .id(1)
                    .firstname("Test")
                    .lastname("Test")
                    .username("TestT")
                    .email("test@example.com")
                    .password("Igw4UQAlfX$E")
                    .bio("I have a real passion for teaching")
                    .location("Cleveland, OH")
                    .company("Code Monkey, LLC")
                    .build();

            when(resultSet.getInt("id")).thenReturn(1);
            when(resultSet.getString("first_name")).thenReturn("Test");
            when(resultSet.getString("last_name")).thenReturn("Test");
            when(resultSet.getString("username")).thenReturn("TestT");
            when(resultSet.getString("email")).thenReturn("test@example.com");
            when(resultSet.getString("password")).thenReturn("Igw4UQAlfX$E");
            when(resultSet.getString("bio")).thenReturn("I have a real passion for teaching");
            when(resultSet.getString("location")).thenReturn("Cleveland, OH");
            when(resultSet.getString("company")).thenReturn("Code Monkey, LLC");

            // Act
            User actual = underTest.mapRow(resultSet, 1);

            // Assert
            assertThat(actual).isEqualTo(expected);
        }
    }
}
