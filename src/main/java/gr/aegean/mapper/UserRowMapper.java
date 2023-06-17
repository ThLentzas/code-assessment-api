package gr.aegean.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import gr.aegean.model.user.User;

import org.springframework.jdbc.core.RowMapper;

public class UserRowMapper implements RowMapper<User> {

    /**
     * @param resultSet the result set containing the data to map.
     * @param rowNum the number of the current row.
     * @return User object mapped from the result set.
     * @throws SQLException if there's an error accessing the result set.
     */
    @Override
    public User mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new User(
                resultSet.getString("email"),
                resultSet.getString("password")
        );
    }
}