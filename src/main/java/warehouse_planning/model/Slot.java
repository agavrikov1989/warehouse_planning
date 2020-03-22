package warehouse_planning.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author agavrikov
 */
public class Slot implements Comparable<Slot> {

    private final long id;
    private final String guid;
    private final long warehouseId;
    private final LocalDate date;
    private final LocalTime timeFrom;
    private final LocalTime timeTo;
    private Integer conveyorId;
    private final LocalDateTime creationTime;

    public Slot(long id, String guid, long warehouseId, LocalDate date, LocalTime timeFrom, LocalTime timeTo,
                Integer conveyorId, LocalDateTime creationTime) {
        this.id = id;
        this.guid = guid;
        this.warehouseId = warehouseId;
        this.date = date;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        this.conveyorId = conveyorId;
        this.creationTime = creationTime;
    }

    public boolean isFree() {
        return conveyorId == null;
    }

    public boolean isBeforeTimeTo(Slot slot) {
        return getLocalDateTimeTo().isBefore(slot.getLocalDateTimeTo());
    }

    public boolean after(Slot slot) {
        return getLocalDateTimeFrom().isEqual(slot.getLocalDateTimeTo()) ||
                getLocalDateTimeFrom().isAfter(slot.getLocalDateTimeTo());
    }

    private LocalDateTime getLocalDateTimeFrom() {
        return LocalDateTime.of(date, timeFrom);
    }

    private LocalDateTime getLocalDateTimeTo() {
        return timeFrom.isAfter(timeTo) ? LocalDateTime.of(date.plusDays(1), timeTo) : LocalDateTime.of(date, timeTo);
    }

    @Override
    public int compareTo(Slot slot) {
        if (getLocalDateTimeFrom().isEqual(slot.getLocalDateTimeFrom())) {
            return getLocalDateTimeTo().compareTo(slot.getLocalDateTimeTo());
        }
        return getLocalDateTimeFrom().compareTo(slot.getLocalDateTimeFrom());
    }

    @Override
    public String toString() {
        return "Slot{" +
                "id=" + id +
                ", warehouseId=" + warehouseId +
                ", date=" + date +
                ", timeFrom=" + timeFrom +
                ", timeTo=" + timeTo +
                ", conveyorId=" + conveyorId +
                ", creationTime=" + creationTime +
                '}';
    }

    public long getId() {
        return id;
    }

    public String getGuid() {
        return guid;
    }

    public long getWarehouseId() {
        return warehouseId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTimeFrom() {
        return timeFrom;
    }

    public LocalTime getTimeTo() {
        return timeTo;
    }

    public Integer getConveyorId() {
        return conveyorId;
    }

    public void setConveyorId(Integer conveyorId) {
        this.conveyorId = conveyorId;
    }
}
