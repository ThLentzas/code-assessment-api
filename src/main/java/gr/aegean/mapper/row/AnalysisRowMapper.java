package gr.aegean.mapper.row;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import org.springframework.jdbc.core.RowMapper;

import gr.aegean.entity.Analysis;


public class AnalysisRowMapper implements RowMapper<Analysis> {
    @Override
    public Analysis mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Date date = resultSet.getDate("created_date");
        LocalDate createdDate = date != null ? date.toLocalDate() : null;

        return new Analysis(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                createdDate);
    }
}