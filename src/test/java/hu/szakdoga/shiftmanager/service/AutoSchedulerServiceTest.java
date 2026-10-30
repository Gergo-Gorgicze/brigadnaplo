package hu.szakdoga.shiftmanager.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A 4 műszakos folytonos üzem és az N–É–P–P rotációs algoritmus matematikai ellenőrzése.
 */
public class AutoSchedulerServiceTest {

    @Test
    @DisplayName("Garantált 24/7/365 lefedettség: Minden nap pontosan 1 nappalos és 1 éjszakás brigád van beosztva")
    public void testContinuousDailyCoverage() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        // 365 nap ellenőrzése egy egész éven át
        for (int i = 0; i < 365; i++) {
            LocalDate testDate = startDate.plusDays(i);

            int dayCount = 0;
            int nightCount = 0;
            int restCount = 0;

            for (int teamId = 1; teamId <= 4; teamId++) {
                String shift = AutoSchedulerService.getShiftCodeForDay(teamId, testDate);
                switch (shift) {
                    case "N": dayCount++; break;
                    case "É": nightCount++; break;
                    case "P": restCount++; break;
                    default: fail("Ismeretlen műszakkód: " + shift);
                }
            }

            assertEquals(1, dayCount, "A(z) " + testDate + " napon pontosan 1 nappalos brigádnak kell lennie!");
            assertEquals(1, nightCount, "A(z) " + testDate + " napon pontosan 1 éjszakás brigádnak kell lennie!");
            assertEquals(2, restCount, "A(z) " + testDate + " napon pontosan 2 pihenő brigádnak kell lennie!");
        }
    }

    @Test
    @DisplayName("Pihenőidő törvényi garancia (Mt. 104. §): Egyetlen brigád sem kaphat nappali műszakot éjszakai után")
    public void testNoDayShiftImmediatelyAfterNightShift() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        for (int teamId = 1; teamId <= 4; teamId++) {
            for (int i = 0; i < 365; i++) {
                LocalDate day1 = startDate.plusDays(i);
                LocalDate day2 = day1.plusDays(1);

                String shift1 = AutoSchedulerService.getShiftCodeForDay(teamId, day1);
                String shift2 = AutoSchedulerService.getShiftCodeForDay(teamId, day2);

                // Ha tegnap éjszakás volt (18-06), ma nem lehet nappalos (06-18), mert 0 óra lenne a pihenő
                if ("É".equals(shift1)) {
                    assertNotEquals("N", shift2, 
                            String.format("Mt. 104. § súlyos szabálysértés a %d. brigádnál: %s (É) után %s (N)!", 
                                    teamId, day1, day2));
                }
            }
        }
    }

    @Test
    @DisplayName("N–É–P–P ciklus: minden brigád nappal után éjszakát, majd 2 teljes pihenőnapot kap")
    public void testRestPeriodDurationAfterNightShifts() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        for (int teamId = 1; teamId <= 4; teamId++) {
            for (int i = 0; i < 365; i++) {
                LocalDate d1 = startDate.plusDays(i);
                if (!"N".equals(AutoSchedulerService.getShiftCodeForDay(teamId, d1))) continue;

                // A nappalos napot éjszaka, majd 2 pihenőnap követi, utána újra nappal indul a ciklus
                assertEquals("É", AutoSchedulerService.getShiftCodeForDay(teamId, d1.plusDays(1)), "Nappal után éjszaka következik");
                assertEquals("P", AutoSchedulerService.getShiftCodeForDay(teamId, d1.plusDays(2)), "1. pihenőnapnak kell lennie az éjszaka után");
                assertEquals("P", AutoSchedulerService.getShiftCodeForDay(teamId, d1.plusDays(3)), "2. pihenőnapnak kell lennie az éjszaka után");
                assertEquals("N", AutoSchedulerService.getShiftCodeForDay(teamId, d1.plusDays(4)), "A 4 napos ciklus újra nappallal indul");
            }
        }
    }
}
