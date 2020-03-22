package warehouse_planning.controller;

import java.io.IOException;
import java.time.*;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import warehouse_planning.service.SlotService;
import warehouse_planning.service.WarehouseService;

/**
 * @author agavrikov
 */
@RestController
@RequestMapping("/warehouses/")
public class WarehouseController {

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
        @RequestParam(required = false, defaultValue = "1") int page) {
        return slotService.getSlotModelAndView(guid, id, conveyorId, date, dateFrom, timeFrom, dateTo, timeTo, page);
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
    public ModelAndView add(@RequestParam String name) {
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
    public ModelAndView download(@PathVariable long id, @RequestParam MultipartFile file)
            throws IOException, InvalidFormatException {
        slotService.download(id, file);
        return new ModelAndView("redirect:/warehouses/" + id + "/slots");
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
