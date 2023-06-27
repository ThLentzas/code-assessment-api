package gr.aegean.repository;

import gr.aegean.mapper.VerificationTokenRowMapper;
import gr.aegean.model.token.TokenType;
import gr.aegean.model.token.VerificationToken;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class VerificationTokenRepository {
    private final JdbcTemplate jdbcTemplate;

    public VerificationTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createPasswordResetToken(VerificationToken verificationToken) {
        final String sql = "INSERT INTO verification_token (" +
                "user_id, " +
                "token, " +
                "expiry_date, " +
                "type) " +
                "VALUES (?, ?, ?, CAST(? AS token_type))";

        jdbcTemplate.update(
                sql,
                verificationToken.getUserId(),
                verificationToken.getToken(),
                verificationToken.getExpiryDate(),
                TokenType.PASSWORD_RESET.name());
    }

    public Optional<VerificationToken> findPasswordResetToken(String token) {
        final String sql = "SELECT " +
                "user_id, " +
                "token, " +
                "expiry_date, " +
                "type " +
                "FROM verification_token " +
                "WHERE token = ? " + "AND type = CAST(? AS token_type)";

        List<VerificationToken> verificationToken = jdbcTemplate.query(
                sql,
                new VerificationTokenRowMapper(),
                token,
                TokenType.PASSWORD_RESET.name());

        return verificationToken.stream().findFirst();
    }

    public void deletePasswordResetToken(String token) {
        final String sql = "DELETE FROM verification_token WHERE token = ? AND type = CAST(? AS token_type)";

        jdbcTemplate.update(sql, token, TokenType.PASSWORD_RESET.name());
    }

    public void deleteAllTokens() {
        final String sql = "DELETE FROM verification_token";

        jdbcTemplate.update(sql);
    }
}
