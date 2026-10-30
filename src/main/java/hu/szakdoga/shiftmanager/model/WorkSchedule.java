package hu.szakdoga.shiftmanager.model;

import java.time.LocalDate;

/**
 * A beosztási rács egy cellája: egy dolgozó műszakja egy adott napon.
 */
public class WorkSchedule {
    private long id;
    private User user;
    private ShiftType shiftType;
    private LocalDate date;
    private String notes;

    public WorkSchedule() {}

    public WorkSchedule(long id, User user, ShiftType shiftType, LocalDate date, String notes) {
        this.id = id;
        this.user = user;
        this.shiftType = shiftType;
        this.date = date;
        this.notes = notes;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ShiftType getShiftType() { return shiftType; }
    public void setShiftType(ShiftType shiftType) { this.shiftType = shiftType; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return (user != null ? user.getFullName() : "N/A") + " @ " + date + " : " + (shiftType != null ? shiftType.getCode() : "-");
    }
}
