package gr.aegean.repository;

import gr.aegean.model.user.UserPrincipal;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

import gr.aegean.model.user.User;
import gr.aegean.exception.BadCredentialsException;
import gr.aegean.mapper.UserRowMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * @return the ID of the newly created user that will be used for the URI.
     */
    public Integer registerUser(User user) {
        final String sql = "INSERT INTO app_user (" +
                "first_name, " +
                "last_name, " +
                "username, " +
                "email, " +
                "password, " +
                "bio, " +
                "location, " +
                "company) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        int insert = jdbcTemplate.update(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, user.getFirstname());
            preparedStatement.setString(2, user.getLastname());
            preparedStatement.setString(3, user.getUsername());
            preparedStatement.setString(4, user.getEmail());
            preparedStatement.setString(5, user.getPassword());
            preparedStatement.setString(6, user.getBio());
            preparedStatement.setString(7, user.getLocation());
            preparedStatement.setString(8, user.getCompany());

            return preparedStatement;
        }, keyHolder);

        Integer id = null;
        if (insert == 1) {
            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null && keys.containsKey("id")) {
                id = (Integer) keys.get("id");
            }
        }

        return id;
    }

    public boolean checkDuplicateEmail(String email) {
        final String sql = "SELECT COUNT(*) FROM app_user WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);

        return count != null && count > 0;
    }

    public boolean checkDuplicateUsername(String username) {
        final String sql = "SELECT COUNT(*) FROM app_user WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);

        return count != null && count > 0;
    }
}