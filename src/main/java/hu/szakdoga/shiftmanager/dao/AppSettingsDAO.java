package hu.szakdoga.shiftmanager.dao;

import hu.szakdoga.shiftmanager.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Egyszerű kulcs–érték tár az alkalmazás beállításaihoz (app_settings tábla).
 * Itt tároljuk az aktív demó helyzet azonosítóját és a szimulált "mai napot",
 * így a program újraindítás után is ugyanabban a helyzetben folytatódik.
 */
public class AppSettingsDAO {

    public static final String KEY_ACTIVE_SCENARIO = "demo.activeScenario";
    public static final String KEY_SIMULATED_TODAY = "demo.simulatedToday";
    public static final String KEY_FOCUS_MONTH = "demo.focusMonth";
    public static final String KEY_MIN_CREW = "demo.minCrewPerShift";
    /** "true", ha a betöltött demó helyzet adatait azóta módosították (mentés vagy távollét rögzítése). */
    public static final String KEY_MODIFIED = "demo.modified";

    public String get(String key) {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            System.err.println("Hiba a beállítás olvasásakor (" + key + "): " + e.getMessage());
        }
        return null;
    }

    public void set(Connection conn, String key, String value) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO app_settings (setting_key, setting_value) VALUES (?, ?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    /** Jelzi, hogy az aktív demó helyzet adatai eltérnek a betöltött eredeti állapottól. */
    public void markDemoModified() {
        if (get(KEY_ACTIVE_SCENARIO) == null) return;
        try (Connection conn = DatabaseManager.getConnection()) {
            set(conn, KEY_MODIFIED, "true");
        } catch (Exception e) {
            System.err.println("Hiba a módosítás jelzésekor: " + e.getMessage());
        }
    }

    public boolean isDemoModified() {
        return "true".equals(get(KEY_MODIFIED));
    }

    public void clearDemoSettings(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM app_settings WHERE setting_key LIKE 'demo.%'");
        }
    }
}
