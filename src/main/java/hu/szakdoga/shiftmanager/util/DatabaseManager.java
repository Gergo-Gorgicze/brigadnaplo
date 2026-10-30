package hu.szakdoga.shiftmanager.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * A helyi SQLite adatbázis egyetlen (Singleton) kapcsolatkészlete.
 * Induláskor létrehozza a sémát; ha a fájl egy korábbi, eltérő szerkezetű programverzióból
 * származik, a régi táblákat eldobja és újra létrehozza őket.
 */
public final class DatabaseManager {

    /** Az adatbázisfájl. A -Dshiftmanager.db.url paraméterrel felülírható (pl. tesztekhez). */
    private static final String DEFAULT_URL = "jdbc:sqlite:shiftmanager.db";

    private static HikariDataSource dataSource;

    static {
        initDataSource();
        initDatabaseSchema();
    }

    private DatabaseManager() {}

    private static synchronized void initDataSource() {
        if (dataSource != null) return;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getProperty("shiftmanager.db.url", DEFAULT_URL));
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setAutoCommit(true);
        // SQLite: a külső kulcsok ellenőrzése kapcsolatonként kapcsolható be
        config.setConnectionInitSql("PRAGMA foreign_keys = ON");
        dataSource = new HikariDataSource(config);
    }

    private static void initDatabaseSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            dropLegacySchemaIfPresent(stmt);

            try (InputStream in = DatabaseManager.class.getResourceAsStream("/db/init_schema.sql")) {
                if (in == null) {
                    throw new IllegalStateException("Az init_schema.sql nem található a classpath-on!");
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.isEmpty()) continue;
                    sb.append(line).append(' ');
                    if (trimmed.endsWith(";")) {
                        stmt.execute(sb.toString());
                        sb.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Az adatbázis inicializálása sikertelen: " + e.getMessage(), e);
        }
    }

    /** A korábbi programverzió (bejelentkezés, szerepkörök) tábláinak eltávolítása. */
    private static void dropLegacySchemaIfPresent(Statement stmt) throws Exception {
        boolean legacy = false;
        try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('roles', 'time_off_requests')")) {
            legacy = rs.next();
        }
        if (legacy) {
            stmt.execute("PRAGMA foreign_keys = OFF");
            for (String table : new String[]{"work_schedule", "time_off_requests", "time_off", "users", "roles", "teams", "shift_types", "app_settings"}) {
                stmt.execute("DROP TABLE IF EXISTS " + table);
            }
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }

    /** A használt JDBC URL (a tesztek ellenőrzik, hogy nem az éles fájlra írnak). */
    public static String getJdbcUrl() {
        return dataSource.getJdbcUrl();
    }

    public static Connection getConnection() throws Exception {
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
