package hu.szakdoga.shiftmanager.model;

import javafx.beans.property.*;

/**
 * Egy munkavállaló éves óranyilvántartási és szabadság-egyenleg modellje.
 * Tartalmazza az éves szabadságkeretet, a felhasznált és hátralévő napokat/órákat,
 * a betegszabadság és táppénz statisztikát, valamint a munkaidőkeret óraegyenlegét.
 */
public class AnnualLeaveBalance {
    private final User user;
    private final int year;

    private final IntegerProperty totalVacationDays = new SimpleIntegerProperty();
    private final DoubleProperty totalVacationHours = new SimpleDoubleProperty();

    private final IntegerProperty usedVacationDays = new SimpleIntegerProperty();
    private final DoubleProperty usedVacationHours = new SimpleDoubleProperty();

    private final IntegerProperty remainingVacationDays = new SimpleIntegerProperty();
    private final DoubleProperty remainingVacationHours = new SimpleDoubleProperty();

    private final IntegerProperty sickLeaveDays = new SimpleIntegerProperty(); // Mt. 126. § szerinti 15 napig
    private final IntegerProperty sickPayDays = new SimpleIntegerProperty();   // 15 napon túli táppénz
    private final IntegerProperty unpaidLeaveDays = new SimpleIntegerProperty();

    private final DoubleProperty normativeHours = new SimpleDoubleProperty(); // Éves törvényes kötelező óraszám
    private final DoubleProperty scheduledHours = new SimpleDoubleProperty(); // Beosztott / ledolgozott órák
    private final DoubleProperty absenceCreditHours = new SimpleDoubleProperty(); // Szabadság/betegség miatt kiesett műszakórák jóváírása
    private final DoubleProperty hourBalance = new SimpleDoubleProperty();    // Keretegyenleg (plusz/mínusz órák)

    public AnnualLeaveBalance(User user, int year) {
        this.user = user;
        this.year = year;
    }

    public User getUser() { return user; }
    public int getYear() { return year; }

    public String getEmployeeName() { return user.getFullName(); }
    public String getTeamName() { return user.getTeam() != null ? user.getTeam().getName() : "-"; }

    public int getTotalVacationDays() { return totalVacationDays.get(); }
    public void setTotalVacationDays(int v) { totalVacationDays.set(v); }
    public IntegerProperty totalVacationDaysProperty() { return totalVacationDays; }

    public double getTotalVacationHours() { return totalVacationHours.get(); }
    public void setTotalVacationHours(double v) { totalVacationHours.set(v); }

    public int getUsedVacationDays() { return usedVacationDays.get(); }
    public void setUsedVacationDays(int v) { usedVacationDays.set(v); }
    public IntegerProperty usedVacationDaysProperty() { return usedVacationDays; }

    public double getUsedVacationHours() { return usedVacationHours.get(); }
    public void setUsedVacationHours(double v) { usedVacationHours.set(v); }

    public int getRemainingVacationDays() { return remainingVacationDays.get(); }
    public void setRemainingVacationDays(int v) { remainingVacationDays.set(v); }
    public IntegerProperty remainingVacationDaysProperty() { return remainingVacationDays; }

    public double getRemainingVacationHours() { return remainingVacationHours.get(); }
    public void setRemainingVacationHours(double v) { remainingVacationHours.set(v); }

    public int getSickLeaveDays() { return sickLeaveDays.get(); }
    public void setSickLeaveDays(int v) { sickLeaveDays.set(v); }
    public IntegerProperty sickLeaveDaysProperty() { return sickLeaveDays; }

    public int getSickPayDays() { return sickPayDays.get(); }
    public void setSickPayDays(int v) { sickPayDays.set(v); }
    public IntegerProperty sickPayDaysProperty() { return sickPayDays; }

    public int getUnpaidLeaveDays() { return unpaidLeaveDays.get(); }
    public void setUnpaidLeaveDays(int v) { unpaidLeaveDays.set(v); }

    public double getNormativeHours() { return normativeHours.get(); }
    public void setNormativeHours(double v) { normativeHours.set(v); }

    public double getScheduledHours() { return scheduledHours.get(); }
    public void setScheduledHours(double v) { scheduledHours.set(v); }

    public double getAbsenceCreditHours() { return absenceCreditHours.get(); }
    public void setAbsenceCreditHours(double v) { absenceCreditHours.set(v); }

    public double getHourBalance() { return hourBalance.get(); }
    public void setHourBalance(double v) { hourBalance.set(v); }
    public DoubleProperty hourBalanceProperty() { return hourBalance; }

    /**
     * Visszaadja a szabadságkeret kihasználtsági arányát (0.0 - 1.0).
     */
    public double getVacationUsageRatio() {
        if (getTotalVacationDays() == 0) return 1.0;
        return (double) getUsedVacationDays() / getTotalVacationDays();
    }
}
