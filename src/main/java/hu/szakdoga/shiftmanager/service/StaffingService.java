package hu.szakdoga.shiftmanager.service;

import hu.szakdoga.shiftmanager.dao.AppSettingsDAO;
import hu.szakdoga.shiftmanager.dao.ScheduleDAO;
import hu.szakdoga.shiftmanager.dao.TimeOffDAO;
import hu.szakdoga.shiftmanager.dao.UserDAO;
import hu.szakdoga.shiftmanager.model.TimeOffRequest;
import hu.szakdoga.shiftmanager.model.User;
import hu.szakdoga.shiftmanager.model.WorkSchedule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Műszaklétszám-ellenőrzés (minimális brigádlétszám).
 *
 * Folyamatos üzemben minden nappalos és éjszakás műszakhoz legalább N fő szükséges
 * (gépkezelők, beállító, minőségellenőr). A szolgáltatás két helyzetet kezel:
 *  1. Távolléti kérelem jóváhagyása előtt megmutatja, mely napokon esne a műszak létszáma
 *     a minimum alá, ha a kérelmezőt is kiengednénk (szabadságnál ez elutasítási ok,
 *     betegségnél csak figyelmeztetés, hiszen a keresőképtelenség nem utasítható el).
 *  2. Egy kész (generált vagy kézzel módosított) havi beosztásban kilistázza a hiányos műszakokat.
 */
public class StaffingService {

    /** Alapértelmezett minimum, ha nincs külön beállítva (az eredeti, kis létszámú mintaadatokhoz). */
    public static final int DEFAULT_MIN_CREW_PER_SHIFT = 2;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd.");
    private static final String[] HU_DAYS = {"H", "K", "Sze", "Cs", "P", "Szo", "V"};

    private final UserDAO userDAO = new UserDAO();
    private final TimeOffDAO timeOffDAO = new TimeOffDAO();
    private final ScheduleDAO scheduleDAO = new ScheduleDAO();
    private final AppSettingsDAO settingsDAO = new AppSettingsDAO();

    /** Egy hiányos műszak adott napon. */
    public static class Shortage {
        private final LocalDate date;
        private final String shiftCode;
        private final int available;
        private final int required;

        public Shortage(LocalDate date, String shiftCode, int available, int required) {
            this.date = date;
            this.shiftCode = shiftCode;
            this.available = available;
            this.required = required;
        }

        public LocalDate getDate() { return date; }
        public String getShiftCode() { return shiftCode; }
        public int getAvailable() { return available; }
        public int getRequired() { return required; }

        @Override
        public String toString() {
            return String.format("%s (%s) – %s: %d fő (minimum %d fő)",
                    date.format(DATE_FMT), HU_DAYS[date.getDayOfWeek().getValue() - 1],
                    shiftLabel(shiftCode), available, required);
        }
    }

    public static String shiftLabel(String code) {
        if ("N".equals(code)) return "Nappal (06–18)";
        if ("É".equals(code)) return "Éjszaka (18–06)";
        return code;
    }

    private static boolean isWorkingCode(String code) {
        return "N".equals(code) || "É".equals(code);
    }

    /** A beállított minimális műszaklétszám (demó helyzetekben 4 fő). */
    public int getMinCrewPerShift() {
        String v = settingsDAO.get(AppSettingsDAO.KEY_MIN_CREW);
        if (v != null) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ignored) { }
        }
        return DEFAULT_MIN_CREW_PER_SHIFT;
    }

    // ------------------------------------------------------------------
    // Adatbázis-alapú ellenőrzés (a felület ezt hívja)
    // ------------------------------------------------------------------

    /**
     * Megmondja, mely napokon kerülne a kérelmező műszakja a minimális létszám alá,
     * ha a [start, end] időszakra távollétet kapna.
     */
    public List<Shortage> checkLeaveImpact(User requester, LocalDate start, LocalDate end) {
        List<User> users = userDAO.getAllUsers();

        List<TimeOffRequest> approved = timeOffDAO.getForPeriod(start, end);

        Map<Integer, Map<LocalDate, String>> dbCodes = new HashMap<>();
        for (WorkSchedule ws : scheduleDAO.getSchedulesForPeriod(start, end)) {
            if (ws.getShiftType() == null) continue;
            dbCodes.computeIfAbsent(ws.getUser().getId(), k -> new HashMap<>())
                    .put(ws.getDate(), ws.getShiftType().getCode());
        }

        return computeLeaveImpact(requester, start, end, users, approved, dbCodes, getMinCrewPerShift());
    }

    // ------------------------------------------------------------------
    // Tiszta (adatbázistól független) számítások – egységtesztelhetők
    // ------------------------------------------------------------------

    /**
     * Egy dolgozó tényleges műszakkódja egy napon:
     * jóváhagyott távollét > mentett beosztás > N–É–P–P rotáció.
     */
    public static String effectiveCode(User u, LocalDate date, List<TimeOffRequest> approved,
                                       Map<Integer, Map<LocalDate, String>> dbCodes) {
        for (TimeOffRequest r : approved) {
            if (r.getUser() != null && r.getUser().getId() == u.getId() && r.coversDate(date)) {
                return AutoSchedulerService.leaveShiftCodeFor(r.getType());
            }
        }
        Map<LocalDate, String> userCodes = dbCodes != null ? dbCodes.get(u.getId()) : null;
        if (userCodes != null && userCodes.containsKey(date)) {
            return userCodes.get(date);
        }
        if (u.getTeam() != null) {
            return AutoSchedulerService.getShiftCodeForDay(u.getTeam().getId(), date);
        }
        return null;
    }

    public static List<Shortage> computeLeaveImpact(User requester, LocalDate start, LocalDate end,
                                                    List<User> users, List<TimeOffRequest> approved,
                                                    Map<Integer, Map<LocalDate, String>> dbCodes, int minCrew) {
        List<Shortage> result = new ArrayList<>();
        if (requester == null || start == null || end == null) return result;

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            String requesterCode = effectiveCode(requester, d, approved, dbCodes);
            if (!isWorkingCode(requesterCode)) continue; // pihenőnapon a távollét nem csökkenti a létszámot

            int available = 0;
            for (User u : users) {
                if (u.getId() == requester.getId()) continue;
                if (requesterCode.equals(effectiveCode(u, d, approved, dbCodes))) {
                    available++;
                }
            }
            if (available < minCrew) {
                result.add(new Shortage(d, requesterCode, available, minCrew));
            }
        }
        return result;
    }

    /**
     * Egy kész beosztásban (pl. az automatikus tervezés vagy a kézi módosítás eredménye)
     * megkeresi azokat a napokat, ahol a nappalos vagy az éjszakás műszak létszáma a minimum alatt van.
     */
    public static List<Shortage> findShortages(List<WorkSchedule> schedules, int minCrew) {
        Map<LocalDate, int[]> counts = new TreeMap<>();
        for (WorkSchedule ws : schedules) {
            if (ws.getDate() == null) continue;
            int[] c = counts.computeIfAbsent(ws.getDate(), k -> new int[2]);
            if (ws.getShiftType() == null) continue;
            if ("N".equals(ws.getShiftType().getCode())) c[0]++;
            else if ("É".equals(ws.getShiftType().getCode())) c[1]++;
        }
        List<Shortage> result = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> e : counts.entrySet()) {
            if (e.getValue()[0] < minCrew) result.add(new Shortage(e.getKey(), "N", e.getValue()[0], minCrew));
            if (e.getValue()[1] < minCrew) result.add(new Shortage(e.getKey(), "É", e.getValue()[1], minCrew));
        }
        return result;
    }

    /** Hiánylista szöveges összefoglalója (legfeljebb maxLines sor). */
    public static String format(List<Shortage> shortages, int maxLines) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Shortage s : shortages) {
            if (n++ >= maxLines) {
                sb.append("  … és további ").append(shortages.size() - maxLines).append(" hiányos műszak\n");
                break;
            }
            sb.append("  • ").append(s).append('\n');
        }
        return sb.toString();
    }
}
