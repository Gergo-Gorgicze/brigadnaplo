package hu.szakdoga.shiftmanager.demo;

import hu.szakdoga.shiftmanager.model.*;
import hu.szakdoga.shiftmanager.service.AnnualTimeTrackingService;
import hu.szakdoga.shiftmanager.service.MtLaborRuleValidator;
import hu.szakdoga.shiftmanager.service.StaffingService;
import hu.szakdoga.shiftmanager.util.AppClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A demó helyzetek adatainak ellenőrzése adatbázis nélkül: a generált egész éves beosztás
 * szabályos, és az egyes helyzetek pontosan azt a programviselkedést váltják ki, amit a leírásuk ígér.
 */
public class DemoScenariosTest {

    private static final Map<String, ShiftType> SHIFTS = ScenarioCatalog.standardShiftTypes();

    @AfterEach
    public void resetClock() {
        AppClock.reset();
    }

    private static ScenarioBuilder.Materialized build(String scenarioId) {
        DemoScenario s = ScenarioCatalog.byId(scenarioId);
        AppClock.setSimulatedToday(s.getSimulatedToday());
        ScenarioBuilder b = new ScenarioBuilder();
        s.define(b);
        return b.materialize(s.getSimulatedToday(), DemoRoster.YEAR, SHIFTS);
    }

    private static Map<Integer, Map<LocalDate, String>> codes(ScenarioBuilder.Materialized m) {
        Map<Integer, Map<LocalDate, String>> map = new HashMap<>();
        for (WorkSchedule ws : m.schedules) {
            map.computeIfAbsent(ws.getUser().getId(), k -> new HashMap<>()).put(ws.getDate(), ws.getShiftType().getCode());
        }
        return map;
    }

    private static List<StaffingService.Shortage> impact(ScenarioBuilder.Materialized m, String key, String from, String to) {
        return StaffingService.computeLeaveImpact(m.user(key), LocalDate.parse(from), LocalDate.parse(to),
                m.users, m.requests, codes(m), 4);
    }

    private static AnnualLeaveBalance balance(ScenarioBuilder.Materialized m, String key) {
        User u = m.user(key);
        List<WorkSchedule> own = new ArrayList<>();
        for (WorkSchedule ws : m.schedules) {
            if (ws.getUser().getId() == u.getId()) own.add(ws);
        }
        return AnnualTimeTrackingService.computeBalance(u, DemoRoster.YEAR, m.requests, own,
                AnnualTimeTrackingService.calculateAnnualNormativeHours(DemoRoster.YEAR), 11.25);
    }

    @Test
    @DisplayName("Minden demó helyzet egész éves beosztása szabályos és minden műszak legalább 4 fős")
    public void testAllScenariosProduceValidFullyStaffedYear() {
        for (DemoScenario s : ScenarioCatalog.all()) {
            ScenarioBuilder.Materialized m = build(s.getId());

            assertEquals(24, m.users.size(), s.getDisplayName());
            assertEquals(24 * 365, m.schedules.size(), s.getDisplayName());

            MtLaborRuleValidator.ValidationResult v = new MtLaborRuleValidator().validateAll(m.schedules);
            assertTrue(v.isValid(), s.getDisplayName() + ": " + v.getErrorMessage());

            List<StaffingService.Shortage> shortages = StaffingService.findShortages(m.schedules, s.getMinCrewPerShift());
            assertTrue(shortages.isEmpty(), s.getDisplayName() + ": " + StaffingService.format(shortages, 10));

            for (User u : m.users) {
                assertTrue(balance(m, keyOf(m, u)).getRemainingVacationDays() >= 0, u.getFullName());
            }
        }
    }

    @Test
    @DisplayName("Egy dolgozónak sincs egymást átfedő távolléte, és jövőbeli betegség nem kerül az adatok közé")
    public void testNoOverlappingOrFutureSickLeaves() {
        for (DemoScenario s : ScenarioCatalog.all()) {
            ScenarioBuilder.Materialized m = build(s.getId());
            for (TimeOffRequest a : m.requests) {
                if ("BETEGSZABADSAG".equals(a.getType())) {
                    assertFalse(a.getStartDate().isAfter(s.getSimulatedToday()), s.getDisplayName());
                }
                for (TimeOffRequest b : m.requests) {
                    if (a == b || a.getUser().getId() != b.getUser().getId()) continue;
                    boolean overlap = !a.getStartDate().isAfter(b.getEndDate()) && !b.getStartDate().isAfter(a.getEndDate());
                    assertFalse(overlap, s.getDisplayName() + ": átfedés " + a.getUser().getFullName());
                }
            }
        }
    }

    @Test
    @DisplayName("1. helyzet: Tamás decemberi szabadsága létszámhiányt okozna, a dec. 14–23. időszak viszont kiadható")
    public void testScenario1() {
        ScenarioBuilder.Materialized m = build("1");
        AnnualLeaveBalance tamas = balance(m, DemoRoster.FEKETE);
        assertEquals(34, tamas.getTotalVacationDays());
        assertEquals(31, tamas.getRemainingVacationDays());
        assertEquals(55.75, tamas.getHourBalance(), 0.001);

        assertEquals(4, impact(m, DemoRoster.FEKETE, "2026-12-02", "2026-12-11").size());
        assertTrue(impact(m, DemoRoster.FEKETE, "2026-12-14", "2026-12-23").isEmpty());

        AnnualLeaveBalance bence = balance(m, DemoRoster.ILLES);
        assertEquals(15, bence.getSickLeaveDays());
        assertEquals(25, bence.getSickPayDays());
    }

    @Test
    @DisplayName("2. helyzet: Balogh Ádám betegsége egy nappalos és egy éjszakás műszakot tesz hiányossá")
    public void testScenario2() {
        ScenarioBuilder.Materialized m = build("2");
        assertEquals(15, balance(m, DemoRoster.BALOGH).getSickLeaveDays());
        List<StaffingService.Shortage> s = impact(m, DemoRoster.BALOGH, "2026-02-09", "2026-02-18");
        assertEquals(2, s.size());
        assertEquals("N", s.get(0).getShiftCode());
        assertEquals("É", s.get(1).getShiftCode());

        MtLaborRuleValidator validator = new MtLaborRuleValidator();
        assertFalse(validator.validateAll(withShift(m, DemoRoster.RACZ, "2026-02-10", "N")).isValid());
        List<WorkSchedule> fixed = withShift(m, DemoRoster.LAKATOS, "2026-02-10", "N");
        setShift(fixed, m.user(DemoRoster.VASS), LocalDate.parse("2026-02-11"), "É");
        assertTrue(validator.validateAll(fixed).isValid());
    }

    @Test
    @DisplayName("3. helyzet: éjszaka utáni reggeli helyettesítés tiltott, a pihenőnapos dolgozóé szabályos")
    public void testScenario3() {
        ScenarioBuilder.Materialized m = build("3");
        assertEquals(2, impact(m, DemoRoster.NEMETH, "2026-03-09", "2026-03-11").size());

        MtLaborRuleValidator validator = new MtLaborRuleValidator();
        assertFalse(validator.validateAll(withShift(m, DemoRoster.BIRO, "2026-03-09", "N")).isValid());
        assertTrue(validator.validateAll(withShift(m, DemoRoster.MOLNAR, "2026-03-09", "N")).isValid());
        assertFalse(validator.validateAll(withShift(m, DemoRoster.BIRO, "2026-03-10", "É")).isValid());
        assertTrue(validator.validateAll(withShift(m, DemoRoster.VARGA, "2026-03-10", "É")).isValid());
    }

    @Test
    @DisplayName("4. helyzet: órakontó – plusz műszakok és fizetés nélküli szabadság")
    public void testScenario4() {
        ScenarioBuilder.Materialized m = build("4");
        assertEquals(100.75, balance(m, DemoRoster.HORVATH).getHourBalance(), 0.001);
        assertEquals(-113.0, balance(m, DemoRoster.TAKACS).getHourBalance(), 0.001);
        assertEquals(15, balance(m, DemoRoster.TAKACS).getUnpaidLeaveDays());
    }

    @Test
    @DisplayName("5. helyzet: Dóra karácsony előtti kérelme ütközik, a műszakcsere szabályos")
    public void testScenario5() {
        ScenarioBuilder.Materialized m = build("5");
        assertEquals(2, impact(m, DemoRoster.FARKAS, "2026-12-22", "2026-12-23").size());
        assertEquals(2, impact(m, DemoRoster.FARKAS, "2026-12-28", "2026-12-31").size());

        List<WorkSchedule> firstIdea = withShift(m, DemoRoster.FARKAS, "2026-12-23", "P");
        setShift(firstIdea, m.user(DemoRoster.MAJOR), LocalDate.parse("2026-12-23"), "É");
        assertFalse(new MtLaborRuleValidator().validateAll(firstIdea).isValid());

        List<WorkSchedule> swapped = withShift(m, DemoRoster.FARKAS, "2026-12-23", "P");
        setShift(swapped, m.user(DemoRoster.MOLNAR), LocalDate.parse("2026-12-23"), "É");
        assertTrue(new MtLaborRuleValidator().validateAll(swapped).isValid());
        assertTrue(StaffingService.findShortages(swapped, 4).isEmpty());
    }

    // --- segédfüggvények ---

    private static String keyOf(ScenarioBuilder.Materialized m, User u) {
        for (Map.Entry<String, User> e : m.usersByKey.entrySet()) {
            if (e.getValue().getId() == u.getId()) return e.getKey();
        }
        throw new IllegalStateException();
    }

    private static List<WorkSchedule> withShift(ScenarioBuilder.Materialized m, String key, String date, String code) {
        List<WorkSchedule> copy = new ArrayList<>();
        for (WorkSchedule ws : m.schedules) {
            copy.add(new WorkSchedule(0, ws.getUser(), ws.getShiftType(), ws.getDate(), ws.getNotes()));
        }
        setShift(copy, m.user(key), LocalDate.parse(date), code);
        return copy;
    }

    private static void setShift(List<WorkSchedule> list, User u, LocalDate date, String code) {
        for (WorkSchedule ws : list) {
            if (ws.getUser().getId() == u.getId() && ws.getDate().equals(date)) {
                ws.setShiftType(SHIFTS.get(code));
            }
        }
    }
}
