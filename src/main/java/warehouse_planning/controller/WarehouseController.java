package warehouse_planning.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import warehouse_planning.model.Slot;
import warehouse_planning.service.SlotService;
import warehouse_planning.service.WarehouseService;

/**
 * @author agavrikov
 */
@RestController
@RequestMapping("/warehouses/")
public class WarehouseController {

    private static final int LIMIT = 10;
    private static final int PAGE_COUNT = 10;

    private final SlotService slotService;
    private final WarehouseService warehouseService;

    @GetMapping(value = "", produces = "text/html")
    public ModelAndView getWarehouses() {
        ModelAndView model = new ModelAndView("warehouse_planning/warehouses");
        model.addObject("warehouses", warehouseService.getWarehouses());
        return model;
    }

    @GetMapping(value = "{id}/slots", produces = "text/html")
    public ModelAndView getWarehouseSlot(@PathVariable long id, @RequestParam(required = false) String guid,
        @RequestParam(required = false) Integer conveyorId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeTo,
        @RequestParam(required = false, defaultValue = "1") int page)
    {
        String viewName = "";
        LocalDateTime dateTimeFrom = null;
        LocalDateTime dateTimeTo = null;
        LocalTime midnight = LocalTime.of(0, 0);

        if (StringUtils.isEmpty(guid) && conveyorId == null && date == null &&
                dateFrom == null && timeFrom == null &&
                dateTo == null && timeTo == null) {
            viewName = "slots";
        } else if (conveyorId != null && StringUtils.isEmpty(guid) && date == null &&
                dateFrom == null && timeFrom == null &&
                dateTo == null && timeTo == null) {
            viewName = "conveyor";
        } else if (date != null && StringUtils.isEmpty(guid) && conveyorId == null &&
                dateFrom == null && timeFrom == null &&
                dateTo == null && timeTo == null) {
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
        model.addObject("warehouseId", id);
        model.addObject("guid", guid);
        model.addObject("conveyorId", conveyorId);
        model.addObject("date", date);
        model.addObject("dateFrom", dateFrom);
        model.addObject("timeFrom", timeFrom);
        model.addObject("dateTo", dateTo);
        model.addObject("timeTo", timeTo);
        model.addObject("slots", slotService.getSlots(id, guid, conveyorId, dateTimeFrom, dateTimeTo, page, LIMIT));
        int maxPage = (int) Math.ceil(
                slotService.getSlotCount(id, guid, conveyorId, dateTimeFrom, dateTimeTo) * 1. / LIMIT);

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

    @GetMapping(value = "{id}/planning")
    @ResponseStatus(HttpStatus.OK)
    public void planning(@PathVariable long id) {
        slotService.planning(id);
    }

    @GetMapping(value = "{id}/clear")
    @ResponseStatus(HttpStatus.OK)
    public void clearConveyor(@PathVariable long id) {
        slotService.clearConveyors(id);
    }

    @PostMapping(value = "/")
    public ModelAndView add(
            @RequestParam String name) {
        warehouseService.save(name);
        return new ModelAndView("redirect:/warehouses/");
    }

    @PostMapping(value = "{id}/slots")
    public ModelAndView addSlot(@PathVariable long id, @RequestParam String guid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeTo) {
        slotService.save(guid, id, date, timeFrom, timeTo);
        return new ModelAndView("redirect:/warehouses/" + id + "/slots");
    }

    @PostMapping(value = "{id}/download")
    public ModelAndView download(@PathVariable long id,
                                 @RequestParam MultipartFile file) throws IOException, InvalidFormatException {

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

            slots.add(new Slot(0, guid, id, date, timeFrom, timeTo, -1, LocalDateTime.now()));
        }
        slotService.saveBatch(slots);
        return new ModelAndView("redirect:/warehouses/" + id + "/slots");
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

    @DeleteMapping(value = "{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteWarehouse(@PathVariable long id) {
        warehouseService.delete(id);
    }

    @DeleteMapping(value = "{id}/slots")
    @ResponseStatus(HttpStatus.OK)
    public void deleteSlots(@PathVariable long id) {
        slotService.deleteSlots(id);
    }

    @DeleteMapping(value = "/{id}/slots/{slotId}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable long id, @PathVariable long slotId) {
        slotService.delete(slotId);
    }

    @Autowired
    public WarehouseController(SlotService slotService, WarehouseService warehouseService) {
        this.slotService = slotService;
        this.warehouseService = warehouseService;
    }
}
