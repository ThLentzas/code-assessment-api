package gr.aegean.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import gr.aegean.model.user.User;
import gr.aegean.mapper.UserRowMapper;
import gr.aegean.exception.ServerErrorException;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper mapper;
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";

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

    /*
        The following methods if they don't update the entity correctly is because some internal error happened
        (BAD SQL GRAMMAR) and not because the user is not found.
     */
    public void updateFirstname(Integer userId, String firstname) {
        final String sql = "UPDATE app_user SET first_name = ? WHERE id = ?";

        int update = jdbcTemplate.update(sql, firstname, userId);

        if (update != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void updateLastname(Integer userId, String lastname) {
        final String sql = "UPDATE app_user SET last_name = ? WHERE id = ?";

        int update = jdbcTemplate.update(sql, lastname, userId);

        if (update != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void updateBio(Integer userId, String bio) {
        final String sql = "UPDATE app_user SET bio = ? WHERE id = ?";

        int update = jdbcTemplate.update(sql, bio, userId);

        if (update != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void updateLocation(Integer userId, String location) {
        final String sql = "UPDATE app_user SET location = ? WHERE id = ?";

        int update = jdbcTemplate.update(sql, location, userId);

        if (update != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void updateCompany(Integer userId, String company) {
        final String sql = "UPDATE app_user SET company = ? WHERE id = ?";

        int update = jdbcTemplate.update(sql, company, userId);

        if (update != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void updateUsername(Integer userId, String username) {
        final String sql = "UPDATE app_user SET username = ? WHERE id = ?";

        int update = jdbcTemplate.update(sql, username, userId);

        if (update != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void updateEmail(Integer userId, String email) {
        final String sql = "UPDATE app_user SET email = ? WHERE id = ?";

        int update = jdbcTemplate.update(sql, email, userId);

        if (update != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public void updatePassword(Integer userId, String password) {
        final String sql = "UPDATE app_user SET password = ? WHERE id = ?";

        int update = jdbcTemplate.update(sql, password, userId);

        if (update != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    /**
     * This method will be used by UsersDetailsService for the user authentication.
     */
    public Optional<User> findUserByEmail(String email) {
        final String sql = "SELECT " +
                "id, " +
                "first_name, " +
                "last_name, " +
                "username, " +
                "email, " +
                "password, " +
                "bio, " +
                "location, " +
                "company FROM app_user WHERE email = ?";

        List<User> users = jdbcTemplate.query(sql, mapper, email);

        return users.stream().findFirst();
    }


    public Optional<User> findUserByUserId(Integer userId) {
        final String sql = "SELECT " +
                "id, " +
                "first_name, " +
                "last_name, " +
                "username, " +
                "email, " +
                "password, " +
                "bio, " +
                "location, " +
                "company FROM app_user WHERE id = ?";

        List<User> users = jdbcTemplate.query(sql, mapper, userId);

        return users.stream().findFirst();
    }

    public boolean existsUserWithEmail(String email) {
        final String sql = "SELECT COUNT(*) FROM app_user WHERE email = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);

        return count != null && count > 0;
    }

    public boolean existsUserWithUsername(String username) {
        final String sql = "SELECT COUNT(*) FROM app_user WHERE username = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);

        return count != null && count > 0;
    }

    public void deleteAllUsers() {
        final String sql = "DELETE FROM app_user";

        jdbcTemplate.update(sql);
    }
}