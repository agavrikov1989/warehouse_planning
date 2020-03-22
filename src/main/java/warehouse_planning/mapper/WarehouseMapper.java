package warehouse_planning.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import warehouse_planning.model.Warehouse;

/**
 * @author agavrikov
 */
public class WarehouseMapper implements RowMapper<Warehouse> {

    @Override
    public Warehouse mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetWrapper rsWrapper = new ResultSetWrapper(rs);
        return new Warehouse(rs.getLong("id"),
            rsWrapper.getLocalDateTime("creation_time"),
            rs.getString("name")
        );
    }
}
