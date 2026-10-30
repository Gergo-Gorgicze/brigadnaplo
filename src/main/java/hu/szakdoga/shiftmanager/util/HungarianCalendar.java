package hu.szakdoga.shiftmanager.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Magyarországi naptári segédfüggvények: munkaszüneti napok és munkanapok számolása.
 * A szabadság és a betegszabadság munkanapokban értendő (Mt. 115. §, Mt. 126. §),
 * ezért a távollétek hosszát az általános munkarend szerinti munkanapokban
 * (hétfő–péntek, munkaszüneti napok nélkül) számoljuk, nem naptári napokban.
 */
public final class HungarianCalendar {

    /** Rögzített dátumú munkaszüneti napok (hónap-nap formátumban). */
    public static final Set<String> OFFICIAL_HOLIDAYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "01-01", "03-15", "05-01", "08-20", "10-23", "11-01", "12-25", "12-26"
    )));

    private HungarianCalendar() {}

    public static boolean isHoliday(LocalDate date) {
        return OFFICIAL_HOLIDAYS.contains(String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth()));
    }

    /** Általános munkarend szerinti munkanap: hétfő–péntek, és nem munkaszüneti nap. */
    public static boolean isWorkday(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !isHoliday(date);
    }

    /** A két dátum (mindkettőt beleértve) közé eső munkanapok száma. */
    public static int countWorkdays(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) return 0;
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (isWorkday(d)) count++;
        }
        return count;
    }
}
