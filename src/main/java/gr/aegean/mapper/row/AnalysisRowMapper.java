package gr.aegean.mapper.row;

import gr.aegean.entity.Analysis;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;


@Service
public class AnalysisRowMapper implements RowMapper<Analysis> {
    @Override
    public Analysis mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp("created_date");
        LocalDateTime createdDate = timestamp != null ? timestamp.toLocalDateTime() : null;

        return new Analysis(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                createdDate
        );
    }
}
