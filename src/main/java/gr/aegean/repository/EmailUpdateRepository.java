package gr.aegean.repository;

import gr.aegean.mapper.row.EmailUpdateTokenRowMapper;
import gr.aegean.entity.EmailUpdateToken;
import gr.aegean.exception.ServerErrorException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    public Optional<EmailUpdateToken> findToken(String token) {
        final String sql = "SELECT " +
                "user_id, " +
                "token, " +
                "email, " +
                "expiry_date " +
                "FROM email_update_token " +
                "WHERE token = ?";

        List<EmailUpdateToken> emailUpdateTokens = jdbcTemplate.query(sql, mapper, token);

        return emailUpdateTokens.stream().findFirst();
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

