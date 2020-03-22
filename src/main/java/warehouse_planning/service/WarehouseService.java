package warehouse_planning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import warehouse_planning.mapper.WarehouseMapper;
import warehouse_planning.model.Warehouse;

import java.util.List;

/**
 * @author agavrikov
 */
@Component
public class WarehouseService {

    private static final String SEQUENCE = "warehouse_seq";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<Warehouse> getWarehouses() {
        String sql =
            "SELECT id, creation_time, name " +
            "  FROM warehouse " +
            " ORDER BY id";
        return jdbcTemplate.query(sql, new WarehouseMapper());
    }

    public List<Warehouse> getWarehouse(long id) {
        String sql =
            "SELECT id, creation_time, name " +
            "  FROM warehouse " +
            " WHERE id = :id " +
            " ORDER BY id";;

        return jdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("id", id),
                new WarehouseMapper());
    }

    public void save(String name) {
        String sql = "INSERT INTO warehouse (id, name) VALUES (nextval('" + SEQUENCE + "'), :name)";
        jdbcTemplate.update(sql, new MapSqlParameterSource().addValue("name", name));
    }


    public void delete(long id) {
        String sql = "DELETE FROM warehouse WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                        .addValue("id", id));
        sql = "DELETE FROM slot WHERE warehouse_id = :warehouse_id";
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("warehouse_id", id));
    }

    @Autowired
    public WarehouseService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
