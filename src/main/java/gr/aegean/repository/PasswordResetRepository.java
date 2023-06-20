package gr.aegean.repository;

import gr.aegean.security.auth.PasswordResetToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
                passwordResetRequest.userId(),
                passwordResetRequest.resetToken(),
                passwordResetRequest.expiryDate());
    }
}
