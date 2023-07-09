package gr.aegean.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import gr.aegean.model.user.User;


class UserRowMapperTest {

    private UserRowMapper mapper;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        mapper = new UserRowMapper();
        resultSet = mock(ResultSet.class);
    }

    @Test
    void shouldMapRowIntoUser() throws SQLException {
        // Arrange
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("first_name")).thenReturn("first_name");
        when(resultSet.getString("last_name")).thenReturn("last_name");
        when(resultSet.getString("username")).thenReturn("username");
        when(resultSet.getString("email")).thenReturn("email");
        when(resultSet.getString("password")).thenReturn("password");
        when(resultSet.getString("bio")).thenReturn("bio");
        when(resultSet.getString("location")).thenReturn("location");
        when(resultSet.getString("company")).thenReturn("company");

        // Act
        User user = mapper.mapRow(resultSet, 1);

        // Assert
        assertThat(user.getId()).isEqualTo(1);
        assertThat(user.getFirstname()).isEqualTo("first_name");
        assertThat(user.getLastname()).isEqualTo("last_name");
        assertThat(user.getUsername()).isEqualTo("username");
        assertThat(user.getEmail()).isEqualTo("email");
        assertThat(user.getPassword()).isEqualTo("password");
        assertThat(user.getBio()).isEqualTo("bio");
        assertThat(user.getLocation()).isEqualTo("location");
        assertThat(user.getCompany()).isEqualTo("company");
    }
}
