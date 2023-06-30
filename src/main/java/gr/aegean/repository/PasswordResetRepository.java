package gr.aegean.repository;

import gr.aegean.mapper.PasswordResetTokenRowMapper;
import gr.aegean.model.token.PasswordResetToken;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordResetRepository {
    private final JdbcTemplate jdbcTemplate;

    public void createToken(PasswordResetToken passwordResetToken) {
        final String sql = "INSERT INTO password_reset_token (" +
                "user_id, " +
                "token, " +
                "expiry_date) "  +
                "VALUES (?, ?, ?)";

        jdbcTemplate.update(
                sql,
                passwordResetToken.userId(),
                passwordResetToken.token(),
                passwordResetToken.expiryDate());
    }

    public Optional<PasswordResetToken> findToken(String token) {
        final String sql = "SELECT " +
                "user_id, " +
                "token, " +
                "expiry_date " +
                "FROM password_reset_token " +
                "WHERE token = ?";

        List<PasswordResetToken> passwordResetToken = jdbcTemplate.query(
                sql,
                new PasswordResetTokenRowMapper(),
                token
        );

        return passwordResetToken.stream().findFirst();
    }

    public void deleteToken(String token) {
        final String sql = "DELETE FROM password_reset_token WHERE token = ?";

        jdbcTemplate.update(sql, token);
    }

    public void deleteAllTokens() {
        final String sql = "DELETE FROM password_reset_token";

        jdbcTemplate.update(sql);
    }
}
