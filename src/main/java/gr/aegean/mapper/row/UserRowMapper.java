package gr.aegean.mapper.row;

import java.sql.ResultSet;
import java.sql.SQLException;

import gr.aegean.entity.User;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;


@Service
public class UserRowMapper implements RowMapper<User> {

    /**
     * @param resultSet the result set containing the data to map.
     * @param rowNum    the number of the current row.
     * @return User object mapped from the result set.
     * @throws SQLException if there's an error accessing the result set.
     */
    @Override
    public User mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("password"),
                resultSet.getString("bio"),
                resultSet.getString("location"),
                resultSet.getString("company")
        );
    }
}