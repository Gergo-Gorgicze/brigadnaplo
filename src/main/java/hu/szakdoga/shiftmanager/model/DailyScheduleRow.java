package hu.szakdoga.shiftmanager.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * A JavaFX TableView-ban megjelenő sor modellje.
 * Tartalmazza a dolgozó adatait, a brigádot és a hónap minden napjára a beosztott műszaktípust.
 */
public class DailyScheduleRow {
    private final User user;
    private final StringProperty employeeName;
    private final StringProperty teamName;
    private final Map<Integer, ShiftType> dailyShifts = new HashMap<>();

    public DailyScheduleRow(User user) {
        this.user = user;
        this.employeeName = new SimpleStringProperty(user.getFullName());
        this.teamName = new SimpleStringProperty(user.getTeam() != null ? user.getTeam().getName() : "-");
    }

    public User getUser() {
        return user;
    }

    public String getEmployeeName() {
        return employeeName.get();
    }

    public StringProperty employeeNameProperty() {
        return employeeName;
    }

    public String getTeamName() {
        return teamName.get();
    }

    public StringProperty teamNameProperty() {
        return teamName;
    }

    public void setShift(int dayOfMonth, ShiftType shiftType) {
        dailyShifts.put(dayOfMonth, shiftType);
    }

    public ShiftType getShift(int dayOfMonth) {
        return dailyShifts.get(dayOfMonth);
    }

    public Map<Integer, ShiftType> getDailyShifts() {
        return dailyShifts;
    }

    public double calculateTotalHours() {
        double sum = 0;
        for (ShiftType shift : dailyShifts.values()) {
            if (shift != null && shift.isWorkingShift()) {
                sum += shift.getNetWorkingHours();
            }
        }
        return sum;
    }

    public int countWorkingDays() {
        int count = 0;
        for (ShiftType shift : dailyShifts.values()) {
            if (shift != null && shift.isWorkingShift()) {
                count++;
            }
        }
        return count;
    }
}
