package hu.szakdoga.shiftmanager.service;

import hu.szakdoga.shiftmanager.model.ShiftType;
import hu.szakdoga.shiftmanager.model.User;
import hu.szakdoga.shiftmanager.model.WorkSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A Munka Törvénykönyve (Mt.) beosztási szabályainak (Mt. 99. § és Mt. 104. §) JUnit 5 egységtesztjei.
 */
public class MtLaborRuleValidatorTest {

    private MtLaborRuleValidator validator;
    private User testUser;
    private ShiftType dayShift;
    private ShiftType nightShift;
    private ShiftType restShift;

    @BeforeEach
    public void setUp() {
        validator = new MtLaborRuleValidator();
        testUser = new User(1, "János", "Kovács", null,
                LocalDate.of(2020, 1, 1), LocalDate.of(1985, 5, 20), 0, false);

        dayShift = new ShiftType(1, "Nappal", "N", "06:00", "18:00", 11.5, "#4CAF50", true);
        nightShift = new ShiftType(2, "Éjszaka", "É", "18:00", "06:00", 11.5, "#1E88E5", true);
        restShift = new ShiftType(3, "Pihenőnap", "P", null, null, 0.0, "#9E9E9E", false);
    }

    @Test
    @DisplayName("Pozitív teszt: Szabályos N–É–P–P műszakváltás sikeresen validál")
    public void testValidSchedulePasses() {
        List<WorkSchedule> schedules = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2026, 3, 1);

        // Két teljes ciklus: Nappal, Éjszaka, Pihenő, Pihenő
        String[] cycle = {"N", "É", "P", "P"};
        for (int i = 0; i < 8; i++) {
            ShiftType st = "N".equals(cycle[i % 4]) ? dayShift : "É".equals(cycle[i % 4]) ? nightShift : restShift;
            schedules.add(new WorkSchedule(i + 1, testUser, st, baseDate.plusDays(i), null));
        }

        MtLaborRuleValidator.ValidationResult result = validator.validateUserSchedule(schedules);
        assertTrue(result.isValid(), "A szabályos N–É–P–P beosztásnak érvényesnek kell lennie!");
        assertTrue(result.getErrors().isEmpty(), "Nem lehet hibaüzenet!");
    }

    @Test
    @DisplayName("Negatív teszt: Mt. 104. § sértés – Éjszakai műszak után közvetlenül nappalos műszak (0 óra pihenő)")
    public void testNightShiftImmediatelyFollowedByDayShiftFails() {
        List<WorkSchedule> schedules = new ArrayList<>();
        LocalDate day1 = LocalDate.of(2026, 3, 10);
        LocalDate day2 = LocalDate.of(2026, 3, 11);

        // Március 10: Éjszaka (18:00 - 06:00 másnap reggel)
        schedules.add(new WorkSchedule(1, testUser, nightShift, day1, null));
        // Március 11: Nappal (06:00 - 18:00) -> 0 óra pihenőidő!
        schedules.add(new WorkSchedule(2, testUser, dayShift, day2, null));

        MtLaborRuleValidator.ValidationResult result = validator.validateUserSchedule(schedules);
        assertFalse(result.isValid(), "Az éjszaka után közvetlen nappalos műszakot tartalmazó beosztást el kell utasítani!");
        assertEquals(1, result.getErrors().size(), "Pontosan egy hibaüzenetnek kell keletkeznie!");
        assertTrue(result.getErrorMessage().contains("Mt. 104. §"), "A hibaüzenetnek hivatkoznia kell az Mt. 104. §-ra!");
    }

    @Test
    @DisplayName("Negatív teszt: Mt. 99. § sértés – 12 órát meghaladó napi munkaidő tiltása")
    public void testDailyWorkingHoursExceeding12HoursFails() {
        List<WorkSchedule> schedules = new ArrayList<>();
        LocalDate date = LocalDate.of(2026, 3, 15);

        // Szabálytalan, 14 órás műszak definiálása
        ShiftType overtimeShift = new ShiftType(99, "Túlóra műszak", "T", "06:00", "20:00", 13.5, "#FF0000", true);
        schedules.add(new WorkSchedule(1, testUser, overtimeShift, date, null));

        MtLaborRuleValidator.ValidationResult result = validator.validateUserSchedule(schedules);
        assertFalse(result.isValid(), "A 12 órát meghaladó napi műszakot el kell utasítani!");
        assertTrue(result.getErrorMessage().contains("Mt. 99. §"), "A hibaüzenetnek hivatkoznia kell az Mt. 99. §-ra!");
    }
}
