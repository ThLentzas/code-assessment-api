package gr.aegean.repository;

import gr.aegean.mapper.EmailUpdateTokenRowMapper;
import gr.aegean.model.token.EmailUpdateToken;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmailUpdateRepository {
    private final JdbcTemplate jdbcTemplate;

    public void createToken(EmailUpdateToken token) {
        final String sql = "INSERT INTO email_update_token (" +
                "user_id, " +
                "token, " +
                "email, " +
                "expiry_date) "  +
                "VALUES (?, ?, ?, ?)";

        jdbcTemplate.update(
                sql,
                token.userId(),
                token.token(),
                token.email(),
                token.expiryDate());
    }

    public Optional<EmailUpdateToken> findToken(String token) {
        final String sql = "SELECT " +
                "user_id, " +
                "token, " +
                "email, " +
                "expiry_date " +
                "FROM email_update_token " +
                "WHERE token = ?";

        List<EmailUpdateToken> emailUpdateTokens = jdbcTemplate.query(
                sql,
                new EmailUpdateTokenRowMapper(),
                token
        );

        return emailUpdateTokens.stream().findFirst();
    }

    public void deleteToken(String token) {
        final String sql = "DELETE FROM email_update_token WHERE token = ?";

        jdbcTemplate.update(sql, token);
    }

    public void deleteAllTokens() {
        final String sql = "DELETE FROM email_update_token";

        jdbcTemplate.update(sql);
    }
}
