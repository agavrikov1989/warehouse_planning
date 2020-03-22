package warehouse_planning.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import warehouse_planning.model.Slot;

/**
 * @author agavrikov
 */
public class SlotMapper implements RowMapper<Slot> {
    @Override
    public Slot mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetWrapper rsWrapper = new ResultSetWrapper(rs);

        return new Slot(rs.getLong("id"), rs.getString("guid"), rs.getLong("warehouse_id"),
            rsWrapper.getLocalDate("date"), rsWrapper.getLocalTime("time_from"),
            rsWrapper.getLocalTime("time_to"), rs.getInt("conveyor_id"),
            rsWrapper.getLocalDateTime("creation_time"));
    }
}
