package warehouse_planning.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import warehouse_planning.algorithm.WarehousePlanning;
import warehouse_planning.mapper.SlotMapper;
import warehouse_planning.model.Slot;

/**
 * @author agavrikov
 */
@Component
public class SlotService {

    private static final String SEQUENCE = "slot_seq";

    private static final int LIMIT = 10;
    private static final int PAGE_COUNT = 10;

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

    public void download(long warehouseId, MultipartFile file) throws IOException, InvalidFormatException {
        InputStream inputStream = file.getInputStream();
        if (!inputStream.markSupported()) {
            inputStream = new PushbackInputStream(inputStream, 8);
        }
        XSSFWorkbook workbook = new XSSFWorkbook(OPCPackage.open(inputStream));
        XSSFSheet sheet = workbook.getSheetAt(0);

        List<Slot> slots = new ArrayList<>();

        for (int i = 0; i < sheet.getLastRowNum(); ++i) {
            XSSFRow row = sheet.getRow(i);
            String guid = row.getCell(0).getStringCellValue();

            LocalDate date = getLocalDateCell(row, 1);
            LocalTime timeFrom = getLocalTimeCell(row, 2);
            LocalTime timeTo = getLocalTimeCell(row, 3);

            if (timeFrom.isAfter(timeTo) || timeFrom.equals(timeTo)) {
                continue;
            }

            slots.add(new Slot(0, guid, warehouseId, date, timeFrom, timeTo, -1, LocalDateTime.now()));
        }
        saveBatch(slots);
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

    public ModelAndView getSlotModelAndView(String guid, long warehouseId, Integer conveyorId, LocalDate date,
        LocalDate dateFrom, LocalTime timeFrom, LocalDate dateTo, LocalTime timeTo, int page)
    {
        String viewName = "";
        LocalDateTime dateTimeFrom = null;
        LocalDateTime dateTimeTo = null;
        LocalTime midnight = LocalTime.of(0, 0);

        if (StringUtils.isEmpty(guid) && conveyorId == null && date == null && dateFrom == null && timeFrom == null &&
                dateTo == null && timeTo == null) {
            viewName = "slots";
        } else if (conveyorId != null && StringUtils.isEmpty(guid) && date == null && dateFrom == null &&
                timeFrom == null && dateTo == null && timeTo == null) {
            viewName = "conveyor";
        } else if (date != null && StringUtils.isEmpty(guid) && conveyorId == null && dateFrom == null &&
                timeFrom == null && dateTo == null && timeTo == null) {
            viewName = "date";
            dateTimeFrom = LocalDateTime.of(date, midnight);
            dateTimeTo = LocalDateTime.of(date.plusDays(1), midnight);
        } else {
            viewName = "search";
            if (dateFrom != null) {
                dateTimeFrom = LocalDateTime.of(dateFrom, timeFrom != null ? timeFrom : midnight);
            }
            if (dateTo != null) {
                dateTimeTo = timeTo != null ? LocalDateTime.of(dateTo, timeTo) : LocalDateTime.of(dateTo.plusDays(1), midnight);
            }
        }

        ModelAndView model = new ModelAndView("warehouse_planning/" + viewName);
        model.addObject("warehouseId", warehouseId);
        model.addObject("guid", guid);
        model.addObject("conveyorId", conveyorId);
        model.addObject("date", date);
        model.addObject("dateFrom", dateFrom);
        model.addObject("timeFrom", timeFrom);
        model.addObject("dateTo", dateTo);
        model.addObject("timeTo", timeTo);
        model.addObject("slots", getSlots(warehouseId, guid, conveyorId, dateTimeFrom, dateTimeTo, page, LIMIT));
        int maxPage = (int) Math.ceil(
                getSlotCount(warehouseId, guid, conveyorId, dateTimeFrom, dateTimeTo) * 1. / LIMIT);

        List<Integer> pages = new ArrayList<>();
        int left, right;
        if (page - PAGE_COUNT / 2 < 1) {
            left = 1;
        } else if (page + PAGE_COUNT / 2 + 1 > maxPage) {
            left = Math.max(maxPage - PAGE_COUNT + 1, 1);
        } else {
            left = page - PAGE_COUNT / 2;
        }
        right = Math.min(left + PAGE_COUNT, maxPage);
        for (int p = left; p <= right; ++p) {
            pages.add(p);
        }
        model.addObject("page", page);
        model.addObject("maxPage", maxPage);
        model.addObject("pages", pages);
        return model;
    }

    private LocalDate getLocalDateCell(XSSFRow row, int cellnum) {
        return Instant.ofEpochMilli(
                row.getCell(cellnum).getDateCellValue().getTime()
        ).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalTime getLocalTimeCell(XSSFRow row, int cellnum) {
        LocalTime dateTime = LocalDateTime.ofInstant(
                row.getCell(cellnum).getDateCellValue().toInstant(), ZoneId.systemDefault()
        ).toLocalTime();
        return LocalTime.of(dateTime.getHour(), dateTime.getMinute());
    }

    @Autowired
    public SlotService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
