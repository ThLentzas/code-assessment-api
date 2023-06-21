package gr.aegean.repository;

import gr.aegean.mapper.PasswordRestTokenRowMapper;
import gr.aegean.security.password.PasswordResetToken;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PasswordResetRepository {
    private final JdbcTemplate jdbcTemplate;

    public PasswordResetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createPasswordResetRequest(PasswordResetToken passwordResetRequest) {
        final String sql = "INSERT INTO password_reset_token (user_id, reset_token, expiry_date) VALUES (?, ?, ?)";

        jdbcTemplate.update(
                sql,
                passwordResetRequest.getUserId(),
                passwordResetRequest.getResetToken(),
                passwordResetRequest.getExpiryDate());
    }

    public Optional<PasswordResetToken> findPasswordResetToken(String token) {
        final String sql = "SELECT user_id, expiry_date FROM password_reset_token WHERE reset_token = ?";

        List<PasswordResetToken> passwordResetToken = jdbcTemplate.query(sql, new PasswordRestTokenRowMapper(), token);

        return passwordResetToken.stream().findFirst();
    }

    public void deletePasswordResetToken(String token) {
        final String sql = "DELETE FROM password_reset_token WHERE reset_token = ?";

        jdbcTemplate.update(sql, token);
    }
}
