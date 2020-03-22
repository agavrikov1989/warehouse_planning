package warehouse_planning.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author agavrikov
 */
@RestController
public class PingController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PingController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/ping")
    public String ping() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return "OK";
    }
}
