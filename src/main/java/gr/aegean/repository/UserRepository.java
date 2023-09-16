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

import gr.aegean.entity.User;
import gr.aegean.mapper.row.UserRowMapper;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper mapper = new UserRowMapper();
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";

    public void registerUser(User user) {
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

        if (insert == 1) {
            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null && keys.containsKey("id")) {
                user.setId((Integer) keys.get("id"));
            }
        }
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

    /*
        Emails are not case-sensitive.
     */
    public boolean existsUserWithEmail(String email) {
        final String sql = "SELECT EXISTS (SELECT 1 FROM app_user WHERE LOWER(email) = LOWER(?))";

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, email));
    }

    /*
        Username is considered case-sensitive. It depends on the business requirements, but in our case we are going to
        it consider as case-sensitive.
     */
    public boolean existsUserWithUsername(String username) {
        final String sql = "SELECT EXISTS (SELECT 1 FROM app_user WHERE username = ?)";

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, username));
    }

    public void deleteAccount(Integer userId) {
        final String sql = "DELETE FROM app_user WHERE id = ?";

        int update = jdbcTemplate.update(sql, userId);

        if (update != 1) {
            throw new ResourceNotFoundException("No account was found with the provided: " + userId);
        }
    }

    public void deleteAllUsers() {
        final String sql = "DELETE FROM app_user";

        jdbcTemplate.update(sql);
    }
}