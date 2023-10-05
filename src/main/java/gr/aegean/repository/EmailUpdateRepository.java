package gr.aegean.repository;

import gr.aegean.mapper.row.EmailUpdateTokenRowMapper;
import gr.aegean.entity.EmailUpdateToken;
import gr.aegean.exception.ServerErrorException;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import lombok.RequiredArgsConstructor;


@Repository
@RequiredArgsConstructor
public class EmailUpdateRepository {
    private final JdbcTemplate jdbcTemplate;
    private final EmailUpdateTokenRowMapper mapper = new EmailUpdateTokenRowMapper();
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";

    public void saveToken(EmailUpdateToken token) {
        final String sql = "INSERT INTO email_update_token (" +
                "user_id, " +
                "token, " +
                "email, " +
                "expiry_date) " +
                "VALUES (?, ?, ?, ?)";

        int insert = jdbcTemplate.update(
                sql,
                token.userId(),
                token.token(),
                token.email(),
                token.expiryDate());
        if (insert != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    /*
        queryForObject() will throw EmptyResultDataAccessException when the query is expected to return a single row,
        but no rows are returned and IncorrectResultSizeDataAccessException when more than one row is returned. It will
        the result object of the required type, or null in case of SQL NULL. By using Optional.ofNullable() we ensure an
        empty optional in case queryForObject() returns null
        EmptyResultDataAccessException extends IncorrectResultSizeDataAccessException so by catching the parent class
        we deal with both cases
     */
    public Optional<EmailUpdateToken> findToken(String token) {
        final String sql = "SELECT " +
                "user_id, " +
                "token, " +
                "email, " +
                "expiry_date " +
                "FROM email_update_token " +
                "WHERE token = ?";

        try {
            EmailUpdateToken emailUpdateToken = jdbcTemplate.queryForObject(sql, mapper, token);

            return Optional.ofNullable(emailUpdateToken);
        } catch (IncorrectResultSizeDataAccessException ire) {
            return Optional.empty();
        }
    }

    public void deleteToken(String token) {
        final String sql = "DELETE FROM email_update_token WHERE token = ?";

        jdbcTemplate.update(sql, token);
    }

    public void deleteAllUserTokens(Integer userId) {
        final String sql = "DELETE FROM email_update_token WHERE user_id = ?";

        jdbcTemplate.update(sql, userId);
    }

    public void deleteAllTokens() {
        final String sql = "DELETE FROM email_update_token";

        jdbcTemplate.update(sql);
    }
}

