package gr.aegean.repository;

import gr.aegean.mapper.row.PasswordResetTokenRowMapper;
import gr.aegean.entity.PasswordResetToken;
import gr.aegean.exception.ServerErrorException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;


@Repository
@RequiredArgsConstructor
public class PasswordResetRepository {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordResetTokenRowMapper mapper = new PasswordResetTokenRowMapper();
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later.";

    public void saveToken(PasswordResetToken passwordResetToken) {
        final String sql = "INSERT INTO password_reset_token (" +
                "user_id, " +
                "token, " +
                "expiry_date) " +
                "VALUES (?, ?, ?)";

        int insert = jdbcTemplate.update(
                sql,
                passwordResetToken.userId(),
                passwordResetToken.token(),
                passwordResetToken.expiryDate());
        if (insert != 1) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public Optional<PasswordResetToken> findToken(String token) {
        final String sql = "SELECT " +
                "user_id, " +
                "token, " +
                "expiry_date " +
                "FROM password_reset_token " +
                "WHERE token = ?";

        List<PasswordResetToken> passwordResetToken = jdbcTemplate.query(sql, mapper, token);

        return passwordResetToken.stream().findFirst();
    }

    public void deleteToken(String token) {
        final String sql = "DELETE FROM password_reset_token WHERE token = ?";

        jdbcTemplate.update(sql, token);

    }

    public void deleteAllUserTokens(Integer userId) {
        final String sql = "DELETE FROM password_reset_token WHERE user_id = ?";

        jdbcTemplate.update(sql, userId);
    }
}
