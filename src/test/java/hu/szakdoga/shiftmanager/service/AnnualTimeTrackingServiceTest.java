package hu.szakdoga.shiftmanager.service;

import hu.szakdoga.shiftmanager.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Az Éves Szabadságkeret és Munkaidőkeret Törvényi Számításának JUnit 5 Tesztjei (Mt. 115-126. §).
 */
public class AnnualTimeTrackingServiceTest {

    @Test
    @DisplayName("Alap- és Életkori Pótszabadság Teszt (Mt. 116-117. §)")
    public void testAgeBasedVacationDays() {
        int currentYear = 2025;

        // 22 éves fiatal munkavállaló (szül: 2003): 20 nap alapszabadság, 0 életkori
        User youngWorker = new User(1, "Bence", "Kovács", null, 
                LocalDate.of(2024, 1, 1), LocalDate.of(2003, 5, 10), 0, false);
        assertEquals(20, youngWorker.calculateTotalAnnualVacationDays(currentYear));

        // 35 éves munkavállaló (szül: 1990): 20 alap + 5 életkori = 25 nap
        User midWorker = new User(2, "Éva", "Nagy", null, 
                LocalDate.of(2020, 1, 1), LocalDate.of(1990, 8, 20), 0, false);
        assertEquals(25, midWorker.calculateTotalAnnualVacationDays(currentYear));

        // 46 éves munkavállaló (szül: 1979): 20 alap + 10 max életkori = 30 nap
        User seniorWorker = new User(3, "László", "Szabó", null, 
                LocalDate.of(2015, 1, 1), LocalDate.of(1979, 2, 14), 0, false);
        assertEquals(30, seniorWorker.calculateTotalAnnualVacationDays(currentYear));
    }

    @Test
    @DisplayName("Gyermekek Utáni Pótszabadság Teszt (Mt. 118. §)")
    public void testChildrenBonusVacationDays() {
        int currentYear = 2025;

        // 30 éves dolgozó 1 gyermekkel: 20 alap + 2 életkori + 2 gyermek = 24 nap
        User oneChild = new User(1, "Anna", "Kiss", null, 
                LocalDate.of(2022, 1, 1), LocalDate.of(1995, 3, 1), 1, false);
        assertEquals(24, oneChild.calculateTotalAnnualVacationDays(currentYear));

        // 30 éves dolgozó 2 gyermekkel: 20 alap + 2 életkori + 4 gyermek = 26 nap
        User twoChildren = new User(2, "Péter", "Tóth", null, 
                LocalDate.of(2022, 1, 1), LocalDate.of(1995, 3, 1), 2, false);
        assertEquals(26, twoChildren.calculateTotalAnnualVacationDays(currentYear));

        // 30 éves dolgozó 3 gyermekkel: 20 alap + 2 életkori + 7 gyermek = 29 nap
        User threeChildren = new User(3, "Gábor", "Molnár", null, 
                LocalDate.of(2022, 1, 1), LocalDate.of(1995, 3, 1), 3, false);
        assertEquals(29, threeChildren.calculateTotalAnnualVacationDays(currentYear));
    }

    @Test
    @DisplayName("Megváltozott Munkaképességű Pótszabadság Teszt (Mt. 120. §)")
    public void testDisabilityBonusVacationDays() {
        int currentYear = 2025;

        // 30 éves dolgozó, nincs gyermek, de fogyatékos (+5 nap): 20 alap + 2 életkori + 5 = 27 nap
        User disWorker = new User(1, "János", "Varga", null, 
                LocalDate.of(2022, 1, 1), LocalDate.of(1995, 3, 1), 0, true);
        assertEquals(27, disWorker.calculateTotalAnnualVacationDays(currentYear));
    }

    @Test
    @DisplayName("Szabadságkeret Óraátváltás (1 munkanap = 8 óra)")
    public void testVacationHoursCalculation() {
        int currentYear = 2025;
        // 25 nap szabadság * 8 óra = 200 óra
        User user = new User(1, "Teszt", "Elek", null, 
                LocalDate.of(2020, 1, 1), LocalDate.of(1990, 1, 1), 0, false);
        assertEquals(200.0, user.calculateTotalAnnualVacationHours(currentYear));
    }
}
