package hu.szakdoga.shiftmanager.service;

import hu.szakdoga.shiftmanager.model.ShiftType;
import hu.szakdoga.shiftmanager.model.WorkSchedule;

import hu.szakdoga.shiftmanager.util.HungarianCalendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Bérpótlék és Munkaóra Kalkulátor a Munka Törvénykönyve (Mt. 140-143. §) alapján.
 */
public class SurchargeCalculator {

    public static class MonthlySummary {
        public double totalWorkHours = 0;
        public double shiftSurchargeHours = 0; // 30% műszakpótlék (18:00 - 06:00)
        public double sundayHours = 0;          // Vasárnapi órák
        public double holidayHours = 0;         // 100% munkaszüneti napi órák
        public int workingDaysCount = 0;
        public int restDaysCount = 0;
        public int leaveDaysCount = 0;
    }

    public MonthlySummary calculate(List<WorkSchedule> schedules) {
        MonthlySummary summary = new MonthlySummary();
        if (schedules == null) return summary;

        for (WorkSchedule ws : schedules) {
            ShiftType shift = ws.getShiftType();
            LocalDate date = ws.getDate();

            if (shift == null) continue;

            // Távollétek: szabadság (SZ), betegség (B), fizetés nélküli szabadság (F)
            if ("SZ".equals(shift.getCode()) || "B".equals(shift.getCode()) || "F".equals(shift.getCode())) {
                summary.leaveDaysCount++;
                continue;
            }

            if (!shift.isWorkingShift()) {
                summary.restDaysCount++;
                continue;
            }

            summary.workingDaysCount++;
            double netHours = shift.getNetWorkingHours();
            summary.totalWorkHours += netHours;

            // Műszakpótlék (Mt. 141. §): 18:00 - 06:00 között
            if ("É".equals(shift.getCode())) {
                // Éjszakai 12 órás műszak teljes egészében a 18:00 - 06:00 sávba esik
                summary.shiftSurchargeHours += netHours;
            }

            // Vasárnapi munkavégzés
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                summary.sundayHours += netHours;
            }

            // Munkaszüneti nap (100% pótlék)
            if (HungarianCalendar.isHoliday(date)) {
                summary.holidayHours += netHours;
            }
        }

        return summary;
    }
}
