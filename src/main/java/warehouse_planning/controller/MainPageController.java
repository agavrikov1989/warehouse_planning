package warehouse_planning.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import warehouse_planning.config.Language;
import warehouse_planning.service.WarehouseService;

/**
 * @author agavrikov
 */
@RestController
public class MainPageController {

    private final WarehouseService warehouseService;

    @GetMapping("/")
    public ModelAndView mainPage(@RequestParam(required = false, defaultValue = "RU") Language language) {
        ModelAndView model = new ModelAndView("warehouse_planning/main");
        model.addObject("warehouses", warehouseService.getWarehouses());
        model.addObject("language", language);
        return model;
    }

    @Autowired
    public MainPageController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }
}
