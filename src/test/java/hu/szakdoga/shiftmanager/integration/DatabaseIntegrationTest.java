package hu.szakdoga.shiftmanager.integration;

import hu.szakdoga.shiftmanager.dao.AppSettingsDAO;
import hu.szakdoga.shiftmanager.dao.ScheduleDAO;
import hu.szakdoga.shiftmanager.dao.TimeOffDAO;
import hu.szakdoga.shiftmanager.dao.UserDAO;
import hu.szakdoga.shiftmanager.demo.ScenarioCatalog;
import hu.szakdoga.shiftmanager.demo.ScenarioLoader;
import hu.szakdoga.shiftmanager.model.AnnualLeaveBalance;
import hu.szakdoga.shiftmanager.model.User;
import hu.szakdoga.shiftmanager.model.WorkSchedule;
import hu.szakdoga.shiftmanager.service.AnnualTimeTrackingService;
import hu.szakdoga.shiftmanager.service.AutoSchedulerService;
import hu.szakdoga.shiftmanager.service.StaffingService;
import hu.szakdoga.shiftmanager.util.AppClock;
import hu.szakdoga.shiftmanager.util.DatabaseManager;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integrációs tesztek valós (ideiglenes) SQLite adatbázissal: séma létrehozása, demó helyzet betöltése,
 * szolgáltatások működése az adatbázisból olvasott adatokon.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseIntegrationTest {

    private static File dbFile;

    @BeforeAll
    public static void setUpDatabase() throws Exception {
        dbFile = File.createTempFile("shiftmanager-test-", ".db");
        dbFile.deleteOnExit();
        System.setProperty("shiftmanager.db.url", "jdbc:sqlite:" + dbFile.getAbsolutePath());
        // Biztonsági ellenőrzés: a teszt soha nem írhat az éles adatbázisfájlba
        Assumptions.assumeTrue(DatabaseManager.getJdbcUrl().contains(dbFile.getName()),
                "Az adatbázis már az éles fájllal inicializálódott, az integrációs teszt kihagyva.");
        new ScenarioLoader().load(ScenarioCatalog.byId("1"));
    }

    @AfterAll
    public static void tearDown() {
        AppClock.reset();
        DatabaseManager.shutdown();
    }

    private User user(String lastName) {
        return new UserDAO().getAllUsers().stream()
                .filter(u -> u.getLastName().equals(lastName))
                .findFirst().orElseThrow();
    }

    @Test
    @Order(1)
    @DisplayName("A demó helyzet betöltése: 24 dolgozó és a teljes év beosztása kerül az adatbázisba")
    public void testScenarioLoaded() {
        assertEquals(24, new UserDAO().countUsers());
        assertEquals(24 * 365, new ScheduleDAO().getSchedulesForYear(2026).size());
        assertEquals(LocalDate.of(2026, 11, 16), AppClock.today());
    }

    @Test
    @Order(2)
    @DisplayName("Az éves egyenleg az adatbázis adataiból ugyanazt adja, mint a tiszta számítás")
    public void testAnnualBalanceFromDatabase() {
        List<AnnualLeaveBalance> balances = new AnnualTimeTrackingService().calculateAnnualBalances(2026);
        AnnualLeaveBalance tamas = balances.stream()
                .filter(b -> b.getUser().getLastName().equals("Fekete")).findFirst().orElseThrow();
        assertEquals(34, tamas.getTotalVacationDays());
        assertEquals(31, tamas.getRemainingVacationDays());
        assertEquals(55.75, tamas.getHourBalance(), 0.001);
    }

    @Test
    @Order(3)
    @DisplayName("Szabadság-kérelem: létszámhiányos időszakban ütközést jelez, más időszakban nem")
    public void testLeaveValidationWithStaffing() {
        AnnualTimeTrackingService service = new AnnualTimeTrackingService();
        User tamas = user("Fekete");

        AnnualTimeTrackingService.LeaveValidationResult blocked = service.validateAndCheckLeaveRequest(
                tamas, LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 11), "SZABADSAG", 2026);
        assertTrue(blocked.isValid());
        assertTrue(blocked.hasConflicts());

        AnnualTimeTrackingService.LeaveValidationResult ok = service.validateAndCheckLeaveRequest(
                tamas, LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 23), "SZABADSAG", 2026);
        assertTrue(ok.isValid());
        assertFalse(ok.hasConflicts());
        assertTrue(ok.getWarnings().isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("Rögzített távollét után az átfedő újabb kérelem elutasításra kerül")
    public void testOverlappingLeaveRejected() {
        User tamas = user("Fekete");
        assertTrue(new TimeOffDAO().save(tamas.getId(), LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 23),
                "SZABADSAG", "Teszt"));
        AnnualTimeTrackingService.LeaveValidationResult again = new AnnualTimeTrackingService().validateAndCheckLeaveRequest(
                tamas, LocalDate.of(2026, 12, 21), LocalDate.of(2026, 12, 22), "SZABADSAG", 2026);
        assertFalse(again.isValid());
    }

    @Test
    @Order(5)
    @DisplayName("A havi automatikus tervezés az adatbázisból: szabályos és a jóváhagyott távolléteket megtartja")
    public void testMonthlyGenerationFromDatabase() {
        List<WorkSchedule> december = new AutoSchedulerService().generateMonthlySchedule(2026, 12);
        assertEquals(24 * 31, december.size());
        User tamas = user("Fekete");
        long leaveDays = december.stream()
                .filter(ws -> ws.getUser().getId() == tamas.getId() && "SZ".equals(ws.getShiftType().getCode()))
                .count();
        assertEquals(10, leaveDays); // dec. 14-23.
        assertTrue(StaffingService.findShortages(december, 4).isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("Kötegelt mentés: hibás rekord esetén a teljes köteg visszagörgetődik")
    public void testBatchRollback() {
        ScheduleDAO dao = new ScheduleDAO();
        List<WorkSchedule> january = dao.getSchedulesForMonth(2026, 1);
        WorkSchedule original = january.get(0);
        String originalCode = original.getShiftType().getCode();

        // Az első rekord módosul, a második érvénytelen (nem létező dolgozó) -> a tranzakció nem véglegesedhet
        WorkSchedule changed = new WorkSchedule(0, original.getUser(),
                january.stream().filter(w -> !w.getShiftType().getCode().equals(originalCode)).findFirst().get().getShiftType(),
                original.getDate(), null);
        User ghost = new User(9999, "Nem", "Létező", null, LocalDate.of(2020, 1, 1), LocalDate.of(1990, 1, 1), 0, false);
        WorkSchedule invalid = new WorkSchedule(0, ghost, original.getShiftType(), original.getDate(), null);

        assertThrows(Exception.class, () -> dao.saveSchedulesBatch(List.of(changed, invalid)));

        WorkSchedule reloaded = dao.getSchedulesForMonth(2026, 1).stream()
                .filter(w -> w.getUser().getId() == original.getUser().getId() && w.getDate().equals(original.getDate()))
                .findFirst().orElseThrow();
        assertEquals(originalCode, reloaded.getShiftType().getCode());
    }

    @Test
    @Order(7)
    @DisplayName("Teljesítmény: 100 dolgozó havi és éves beosztásának generálása és mentése")
    public void testPerformanceWith100Employees() throws Exception {
        try (java.sql.Connection c = DatabaseManager.getConnection(); java.sql.Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM work_schedule");
            st.executeUpdate("DELETE FROM time_off");
            st.executeUpdate("DELETE FROM users");
            for (int i = 1; i <= 100; i++) {
                st.executeUpdate("INSERT INTO users (id, first_name, last_name, team_id, contract_start, birth_date) VALUES ("
                        + i + ", 'Dolgozó" + i + "', 'Teszt', " + (1 + (i - 1) % 4) + ", '2020-01-01', '1990-01-01')");
            }
        }
        AutoSchedulerService scheduler = new AutoSchedulerService();
        ScheduleDAO dao = new ScheduleDAO();
        scheduler.generateMonthlySchedule(2026, 1); // bemelegítés (JIT, kapcsolatkészlet)

        long t0 = System.nanoTime();
        List<WorkSchedule> month = scheduler.generateMonthlySchedule(2026, 3);
        long t1 = System.nanoTime();
        dao.saveSchedulesBatch(month);
        long t2 = System.nanoTime();

        List<WorkSchedule> year = new java.util.ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            year.addAll(scheduler.generateMonthlySchedule(2026, m));
        }
        long t3 = System.nanoTime();
        dao.saveSchedulesBatch(year);
        long t4 = System.nanoTime();

        System.out.printf("100 dolgozó, 1 hónap (%d rekord): generálás %.1f ms, mentés %.1f ms%n",
                month.size(), (t1 - t0) / 1e6, (t2 - t1) / 1e6);
        System.out.printf("100 dolgozó, 12 hónap (%d rekord): generálás %.1f ms, mentés %.1f ms%n",
                year.size(), (t3 - t2) / 1e6, (t4 - t3) / 1e6);

        assertEquals(3100, month.size());
        assertEquals(36500, year.size());
        assertTrue((t2 - t0) / 1e6 < 1000, "NFR-01: a havi generálás és mentés 1 másodpercen belül");
    }

    @Test
    @Order(8)
    @DisplayName("Demó visszaállítás: az aktív helyzet újbóli betöltése törli a bemutatás közbeni módosításokat")
    public void testScenarioResetRestoresOriginalState() throws Exception {
        // Az előző tesztek rögzítettek egy szabadságot, a teljesítményteszt pedig minden dolgozót lecserélt
        AppSettingsDAO settings = new AppSettingsDAO();
        settings.markDemoModified();
        assertTrue(settings.isDemoModified());

        new ScenarioLoader().load(ScenarioCatalog.byId("1"));

        assertFalse(settings.isDemoModified());
        assertEquals("1", settings.get(AppSettingsDAO.KEY_ACTIVE_SCENARIO));
        assertEquals(24, new UserDAO().countUsers());
        User tamas = user("Fekete");
        assertTrue(new TimeOffDAO().getForMonth(2026, 12).stream()
                .noneMatch(t -> t.getUser().getId() == tamas.getId()), "A teszt közben rögzített szabadság törlődött");
        assertEquals(24 * 365, new ScheduleDAO().getSchedulesForYear(2026).size());
    }
}
