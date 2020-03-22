package warehouse_planning.model;

import java.time.LocalDateTime;

/**
 * @author agavrikov
 */
public class Warehouse {

    private final long id;
    private final LocalDateTime creationTime;
    private String name;

    public Warehouse(long id, LocalDateTime creationTime, String name) {
        this.id = id;
        this.creationTime = creationTime;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
