package hu.szakdoga.shiftmanager.util;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Az alkalmazás "mai napja".
 * Normál működésben a rendszeróra dátumát adja vissza, demó helyzet betöltésekor
 * viszont egy szimulált dátumot (pl. 2026. november 16.), hogy a helyzet minden
 * időfüggő szabálya (15 napos előzetes bejelentés, életkor, aktuális hónap)
 * reprodukálhatóan ugyanúgy viselkedjen, bármikor is futtatjuk a programot.
 */
public final class AppClock {
    private static LocalDate simulatedToday;

    private AppClock() {}

    public static LocalDate today() {
        return simulatedToday != null ? simulatedToday : LocalDate.now();
    }

    public static YearMonth currentMonth() {
        return YearMonth.from(today());
    }

    public static boolean isSimulated() {
        return simulatedToday != null;
    }

    public static void setSimulatedToday(LocalDate date) {
        simulatedToday = date;
    }

    public static void reset() {
        simulatedToday = null;
    }
}
