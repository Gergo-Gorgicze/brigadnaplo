package hu.szakdoga.shiftmanager.demo;

import hu.szakdoga.shiftmanager.dao.AppSettingsDAO;
import hu.szakdoga.shiftmanager.dao.ShiftTypeDAO;
import hu.szakdoga.shiftmanager.model.ShiftType;
import hu.szakdoga.shiftmanager.model.TimeOffRequest;
import hu.szakdoga.shiftmanager.model.User;
import hu.szakdoga.shiftmanager.model.WorkSchedule;
import hu.szakdoga.shiftmanager.util.AppClock;
import hu.szakdoga.shiftmanager.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * Egy demó helyzet adatainak betöltése a program adatbázisába.
 *
 * A betöltés egyetlen tranzakcióban kiüríti a dolgozói, beosztási és távolléti táblákat,
 * beírja a helyzet dolgozóit, távolléteit és egész éves beosztását, végül elmenti az aktív
 * helyzetet és a szimulált mai napot. Hiba esetén minden visszagörgetődik.
 *
 * Ugyanennek a helyzetnek az ismételt betöltése visszaállítja az eredeti állapotot: a bemutatás
 * közben rögzített távollétek és kézi módosítások törlődnek, így a helyzet többször is bemutatható.
 */
public class ScenarioLoader {

    private final ShiftTypeDAO shiftTypeDAO = new ShiftTypeDAO();
    private final AppSettingsDAO settingsDAO = new AppSettingsDAO();

    public void load(DemoScenario scenario) throws Exception {
        Map<String, ShiftType> shiftMap = shiftTypeDAO.getShiftTypesByCodeMap();
        for (String code : new String[]{"N", "É", "P", "SZ", "B", "F"}) {
            if (!shiftMap.containsKey(code)) {
                throw new IllegalStateException("Hiányzó műszaktípus az adatbázisban: " + code);
            }
        }

        LocalDate today = scenario.getSimulatedToday();
        ScenarioBuilder builder = new ScenarioBuilder();
        scenario.define(builder);
        ScenarioBuilder.Materialized data = builder.materialize(today, DemoRoster.YEAR, shiftMap);

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("DELETE FROM work_schedule");
                    st.executeUpdate("DELETE FROM time_off");
                    st.executeUpdate("DELETE FROM users");
                    st.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('users', 'time_off', 'work_schedule')");
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users (id, first_name, last_name, team_id, contract_start, birth_date, " +
                        "children_count, has_disability) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    for (User u : data.users) {
                        ps.setInt(1, u.getId());
                        ps.setString(2, u.getFirstName());
                        ps.setString(3, u.getLastName());
                        ps.setInt(4, u.getTeam().getId());
                        ps.setString(5, u.getContractStart().toString());
                        ps.setString(6, u.getBirthDate().toString());
                        ps.setInt(7, u.getChildrenCount());
                        ps.setInt(8, u.isHasDisability() ? 1 : 0);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO time_off (id, user_id, start_date, end_date, type, reason) VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (TimeOffRequest r : data.requests) {
                        ps.setInt(1, r.getId());
                        ps.setInt(2, r.getUser().getId());
                        ps.setString(3, r.getStartDate().toString());
                        ps.setString(4, r.getEndDate().toString());
                        ps.setString(5, r.getType());
                        ps.setString(6, r.getReason());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO work_schedule (user_id, shift_type_id, date, notes) VALUES (?, ?, ?, ?)")) {
                    for (WorkSchedule ws : data.schedules) {
                        ps.setInt(1, ws.getUser().getId());
                        ps.setInt(2, ws.getShiftType().getId());
                        ps.setString(3, ws.getDate().toString());
                        ps.setString(4, ws.getNotes());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                settingsDAO.clearDemoSettings(conn);
                settingsDAO.set(conn, AppSettingsDAO.KEY_ACTIVE_SCENARIO, scenario.getId());
                settingsDAO.set(conn, AppSettingsDAO.KEY_SIMULATED_TODAY, today.toString());
                settingsDAO.set(conn, AppSettingsDAO.KEY_FOCUS_MONTH, scenario.getFocusMonth().toString());
                settingsDAO.set(conn, AppSettingsDAO.KEY_MIN_CREW, String.valueOf(scenario.getMinCrewPerShift()));

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        AppClock.setSimulatedToday(today);
    }

    /**
     * Induláskor visszaállítja a korábban betöltött helyzetet (szimulált dátummal).
     * Üres adatbázis esetén (első indítás) betölti az alapállapotot.
     */
    public DemoScenario restoreOrLoadDefault() throws Exception {
        DemoScenario active = ScenarioCatalog.byId(settingsDAO.get(AppSettingsDAO.KEY_ACTIVE_SCENARIO));
        if (active == null) {
            active = ScenarioCatalog.byId("0");
            load(active);
            return active;
        }
        String today = settingsDAO.get(AppSettingsDAO.KEY_SIMULATED_TODAY);
        AppClock.setSimulatedToday(today != null ? LocalDate.parse(today) : active.getSimulatedToday());
        return active;
    }

    /** Igaz, ha az aktív helyzet adatait a betöltés óta módosították (mentés vagy távollét rögzítése). */
    public boolean isActiveScenarioModified() {
        return settingsDAO.isDemoModified();
    }

    /** A beosztás nézet kezdőhónapja az aktív helyzetben. */
    public YearMonth getFocusMonth(DemoScenario active) {
        String v = settingsDAO.get(AppSettingsDAO.KEY_FOCUS_MONTH);
        if (v != null) {
            try {
                return YearMonth.parse(v);
            } catch (Exception ignored) { }
        }
        return active.getFocusMonth();
    }
}
