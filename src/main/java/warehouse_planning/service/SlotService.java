package warehouse_planning.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import warehouse_planning.algorithm.WarehousePlanning;
import warehouse_planning.mapper.SlotMapper;
import warehouse_planning.model.Slot;

/**
 * @author agavrikov
 */
@Component
public class SlotService {

    private static final String SEQUENCE = "slot_seq";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<Slot> getSlots(long warehousesId) {
        String sql =
            "SELECT id, guid, warehouse_id, date, time_from, time_to, conveyor_id, creation_time " +
            "  FROM slot " +
            " WHERE warehouse_id = :warehouse_id " +
            " ORDER BY conveyor_id, date, time_from";
        return jdbcTemplate.query(sql, new MapSqlParameterSource().addValue("warehouse_id", warehousesId),
                new SlotMapper());
    }

    public List<Slot> getSlots(long warehousesId, String guid, Integer conveyorId, LocalDateTime timeFrom,
                               LocalDateTime timeTo, int page, int limit) {
        String sql =
        "SELECT id, guid, warehouse_id, date, time_from, time_to, conveyor_id, creation_time " +
        "  FROM (" +
        "       SELECT id, guid, warehouse_id, date, time_from, time_to, conveyor_id, creation_time, " +
        "                  ROW_NUMBER() OVER(ORDER BY conveyor_id, date, time_from, time_to) AS rn " +
        "         FROM slot " +
        "        WHERE warehouse_id = :warehouse_id " +
        "          AND (:guid::text IS NULL OR :guid::text = '' OR :guid = guid) " +
        "          AND (:conveyor_id::int IS NULL OR :conveyor_id = conveyor_id) " +
        "          AND (:time_from::timestamp IS NULL OR :time_from <= date + time_from) " +
        "          AND (:time_to::timestamp IS NULL OR CASE WHEN time_from < time_to THEN date + time_to ELSE date + 1 + time_to END <= :time_to) " +
        "       ) AS t " +
        " WHERE rn >= :rn_from AND rn <= :rn_to";

        return jdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("warehouse_id", warehousesId)
                .addValue("guid", guid)
                .addValue("conveyor_id", conveyorId)
                .addValue("time_from", timeFrom)
                .addValue("time_to", timeTo)
                .addValue("page", page)
                .addValue("rn_from", (page - 1) * limit + 1)
                .addValue("rn_to", page * limit),
                new SlotMapper());
    }

    public int getSlotCount(long warehouseId, String guid, Integer conveyorId, LocalDateTime timeFrom,
                            LocalDateTime timeTo) {
        String sql =
            "SELECT COUNT (*) " +
            "  FROM slot " +
            " WHERE warehouse_id = :warehouse_id " +
            "   AND (:guid::text IS NULL OR :guid::text = '' OR :guid = guid) " +
            "   AND (:conveyor_id::int IS NULL OR :conveyor_id = conveyor_id) " +
            "   AND (:time_from::timestamp IS NULL OR :time_from <= date + time_from) " +
            "   AND (:time_to::timestamp IS NULL OR CASE WHEN time_from < time_to THEN date + time_to ELSE date + 1 + time_to END <= :time_to) ";
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
                        .addValue("warehouse_id", warehouseId)
                        .addValue("guid", guid)
                        .addValue("conveyor_id", conveyorId)
                        .addValue("time_from", timeFrom)
                        .addValue("time_to", timeTo), Integer.class);
    }

    public void save(String guid, long warehouseId, LocalDate date, LocalTime timeFrom, LocalTime timeTo) {
        String sql =
            "INSERT INTO slot (id, guid, warehouse_id, date, time_from, time_to, conveyor_id) " +
            "VALUES (nextval('" + SEQUENCE + "'), :guid, :warehouse_id, :date, :time_from, :time_to, NULL)";
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                        .addValue("guid", guid)
                        .addValue("warehouse_id", warehouseId)
                        .addValue("date", date)
                        .addValue("time_from", timeFrom)
                        .addValue("time_to", timeTo));
    }

    public void saveBatch(List<Slot> slots) {
        String sql = "INSERT INTO slot (id, guid, warehouse_id, date, time_from, time_to, conveyor_id) " +
                     "VALUES (nextval('" + SEQUENCE + "'), :guid, :warehouseId, :date, :timeFrom, :timeTo, NULL)";
        List<Map<String, Object>> batchValues = new ArrayList<>(slots.size());
        slots.forEach(slot ->
            batchValues.add(
                new MapSqlParameterSource("guid", slot.getGuid())
                    .addValue("warehouseId", slot.getWarehouseId())
                    .addValue("date", slot.getDate())
                    .addValue("timeFrom", slot.getTimeFrom())
                    .addValue("timeTo", slot.getTimeTo())
                    .getValues()
            )
        );
        jdbcTemplate.batchUpdate(sql, batchValues.toArray(new Map[slots.size()]));
    }


    public void delete(long id) {
        String sql = "DELETE FROM slot WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource().addValue("id", id));
    }

    public void updateConveyors(List<Slot> slots) {
        String sql = "UPDATE slot SET conveyor_id = :conveyorId WHERE id = :id";
        List<Map<String, Object>> batchValues = new ArrayList<>(slots.size());
        slots.forEach(slot ->
            batchValues.add(
                new MapSqlParameterSource("conveyorId", slot.getConveyorId())
                    .addValue("id", slot.getId())
                    .getValues()
            )
        );
        jdbcTemplate.batchUpdate(sql, batchValues.toArray(new Map[slots.size()]));
    }

    public void clearConveyors(long warehouseId) {
        String sql = "UPDATE slot SET conveyor_id = NULL WHERE warehouse_id = :warehouse_id";
        jdbcTemplate.update(sql, new MapSqlParameterSource().addValue("warehouse_id", warehouseId));
    }

    public void deleteSlots(long warehouseId) {
        String sql = "DELETE FROM slot WHERE warehouse_id = :warehouse_id";
        jdbcTemplate.update(sql, new MapSqlParameterSource().addValue("warehouse_id", warehouseId));
    }

    public void planning(long warehouseId) {
        List<Slot> slots = getSlots(warehouseId).stream()
            .sorted()
            .collect(Collectors.toList());
        WarehousePlanning.planning(slots);
        updateConveyors(slots);
    }

    @Autowired
    public SlotService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
