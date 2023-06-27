package gr.aegean.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import gr.aegean.model.token.TokenType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import gr.aegean.model.token.VerificationToken;

@Service
public class VerificationTokenRowMapper implements RowMapper<VerificationToken> {
    @Override
    public VerificationToken mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp("expiry_date");
        LocalDateTime expiryDate = timestamp != null ? timestamp.toLocalDateTime() : null;

        return new VerificationToken(
                resultSet.getInt("user_id"),
                resultSet.getString("token"),
                expiryDate,
                TokenType.valueOf(resultSet.getString("type"))
        );
    }
}
