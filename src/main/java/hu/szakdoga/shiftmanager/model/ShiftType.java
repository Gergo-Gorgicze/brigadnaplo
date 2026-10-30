package hu.szakdoga.shiftmanager.model;

import java.time.LocalTime;

public class ShiftType {
    private int id;
    private String name;
    private String code;
    private LocalTime startTime;
    private LocalTime endTime;
    private String colorCode;
    private boolean isWorkingShift;
    private int breakMinutes;
    private Double netHoursOverride; // csak explicit megadás esetén (pl. egységtesztekben)

    public ShiftType() {}

    public ShiftType(int id, String name, String code, LocalTime startTime, LocalTime endTime, String colorCode, boolean isWorkingShift, int breakMinutes) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.startTime = startTime;
        this.endTime = endTime;
        this.colorCode = colorCode;
        this.isWorkingShift = isWorkingShift;
        this.breakMinutes = breakMinutes;
    }

    /**
     * Egyszerűsített konstruktor egységtesztekhez: a nettó munkaórát közvetlenül adjuk meg
     * (így pl. egy szabálytalan, 12 óránál hosszabb műszak is modellezhető).
     */
    public ShiftType(int id, String name, String code, String startTime, String endTime,
                     double netWorkingHours, String colorCode, boolean isWorkingShift) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.startTime = startTime != null ? LocalTime.parse(startTime) : null;
        this.endTime = endTime != null ? LocalTime.parse(endTime) : null;
        this.colorCode = colorCode;
        this.isWorkingShift = isWorkingShift;
        this.netHoursOverride = netWorkingHours;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public boolean isWorkingShift() {
        return isWorkingShift;
    }

    public void setWorkingShift(boolean workingShift) {
        isWorkingShift = workingShift;
    }

    public int getBreakMinutes() {
        return breakMinutes;
    }

    public void setBreakMinutes(int breakMinutes) {
        this.breakMinutes = breakMinutes;
    }

    /**
     * Számított tényleges munkaórák a munkaközi szünet levonásával (Mt. 103. §)
     */
    public double getNetWorkingHours() {
        if (!isWorkingShift) return 0.0;
        if (netHoursOverride != null) return netHoursOverride;
        return 12.0 - (breakMinutes / 60.0);
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
