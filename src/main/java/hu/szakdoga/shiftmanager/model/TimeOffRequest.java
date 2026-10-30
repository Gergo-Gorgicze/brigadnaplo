package hu.szakdoga.shiftmanager.model;

import java.time.LocalDate;

/**
 * Jóváhagyott távollét: szabadság, betegség (betegszabadság/táppénz) vagy fizetés nélküli szabadság.
 * A program csak a már elbírált (jóváhagyott) távolléteket rögzíti.
 */
public class TimeOffRequest {
    public static final String SZABADSAG = "SZABADSAG";
    public static final String BETEGSZABADSAG = "BETEGSZABADSAG";
    public static final String FIZETETLEN = "FIZETETLEN";

    private int id;
    private User user;
    private LocalDate startDate;
    private LocalDate endDate;
    private String type;
    private String reason;

    public TimeOffRequest() {}

    public TimeOffRequest(int id, User user, LocalDate startDate, LocalDate endDate, String type, String reason) {
        this.id = id;
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.reason = reason;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean coversDate(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
