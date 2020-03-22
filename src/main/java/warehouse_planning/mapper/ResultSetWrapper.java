package warehouse_planning.mapper;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author agavrikov
 */
public final class ResultSetWrapper {

    private final ResultSet rs;

    ResultSetWrapper(ResultSet rs) {
        this.rs = rs;
    }

    LocalDateTime getLocalDateTime(String columnName) throws SQLException {
        Timestamp value = rs.getTimestamp(columnName);
        return rs.wasNull() ? null : value.toLocalDateTime();
    }

    LocalDate getLocalDate(String columnName) throws SQLException {
        Date value = rs.getDate(columnName);
        return rs.wasNull() ? null : value.toLocalDate();
    }

    LocalTime getLocalTime(String columnName) throws SQLException {
        Time value = rs.getTime(columnName);
        return rs.wasNull() ? null : value.toLocalTime();
    }
}
