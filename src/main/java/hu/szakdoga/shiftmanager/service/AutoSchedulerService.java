package hu.szakdoga.shiftmanager.service;

import hu.szakdoga.shiftmanager.dao.ShiftTypeDAO;
import hu.szakdoga.shiftmanager.dao.TimeOffDAO;
import hu.szakdoga.shiftmanager.dao.UserDAO;
import hu.szakdoga.shiftmanager.model.ShiftType;
import hu.szakdoga.shiftmanager.model.TimeOffRequest;
import hu.szakdoga.shiftmanager.model.User;
import hu.szakdoga.shiftmanager.model.WorkSchedule;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 4 Műszakos 12 órás Folytonos Munkarend Automatikus Beosztástervező Motor.
 * Az N–É–P–P (nappal–éjszaka–pihenő–pihenő) rotációs mintát alkalmazza ([D, É, P, P]),
 * figyelembe véve a dolgozók brigádját és a jóváhagyott szabadságokat.
 */
public class AutoSchedulerService {
    private final ShiftTypeDAO shiftTypeDAO = new ShiftTypeDAO();
    private final TimeOffDAO timeOffDAO = new TimeOffDAO();
    private final UserDAO userDAO = new UserDAO();

    // Referencia bázisdátum a 4 napos ciklushoz
    private static final LocalDate CYCLE_EPOCH = LocalDate.of(2025, 1, 1);

    // Alap rotációs minta: 1 nappal, 1 éjszaka, 2 pihenő (12 órás műszakok)
    private static final String[] BASE_CYCLE = {"N", "É", "P", "P"};

    /**
     * Automatikus beosztás generálása a megadott évre és hónapra az összes dolgozóra.
     */
    public List<WorkSchedule> generateMonthlySchedule(int year, int month) {
        Map<String, ShiftType> shiftMap = shiftTypeDAO.getShiftTypesByCodeMap();
        List<User> users = userDAO.getAllUsers();
        List<TimeOffRequest> approvedLeaves = timeOffDAO.getForMonth(year, month);
        return buildMonthlySchedule(users, approvedLeaves, shiftMap, year, month);
    }

    /**
     * A tervező motor adatbázistól független (tiszta) magja: a dolgozók, a jóváhagyott
     * távollétek és a műszaktípusok alapján felépíti egy hónap beosztását.
     * A demó helyzetek adatgenerátora és az egységtesztek is ezt használják.
     */
    public static List<WorkSchedule> buildMonthlySchedule(List<User> users, List<TimeOffRequest> approvedLeaves,
                                                          Map<String, ShiftType> shiftMap, int year, int month) {
        List<WorkSchedule> schedules = new ArrayList<>();
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();

        for (User user : users) {
            if (user.getTeam() == null) continue;
            int teamId = user.getTeam().getId();

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate currentDate = LocalDate.of(year, month, day);

                // 1. Prioritás: Jóváhagyott távollét (szabadság / betegség / fizetés nélküli)
                TimeOffRequest leave = null;
                for (TimeOffRequest l : approvedLeaves) {
                    if (l.getUser().getId() == user.getId() && l.coversDate(currentDate)) {
                        leave = l;
                        break;
                    }
                }

                ShiftType assignedShift;
                String notes = "";

                if (leave != null) {
                    String leaveCode = leaveShiftCodeFor(leave.getType());
                    assignedShift = shiftMap.containsKey(leaveCode) ? shiftMap.get(leaveCode) : shiftMap.get("SZ");
                    notes = "Jóváhagyott távollét";
                } else {
                    // 2. Prioritás: N–É–P–P ciklikus rotációs minta számítása
                    String shiftCode = getShiftCodeForDay(teamId, currentDate);
                    assignedShift = shiftMap.get(shiftCode);
                }

                schedules.add(new WorkSchedule(0, user, assignedShift, currentDate, notes));
            }
        }
        return schedules;
    }

    /**
     * A távollét típusához tartozó műszakkód: szabadság = SZ, betegség = B, fizetés nélküli = F.
     */
    public static String leaveShiftCodeFor(String leaveType) {
        if (leaveType == null) return "SZ";
        switch (leaveType.toUpperCase()) {
            case "BETEGSZABADSAG": return "B";
            case "FIZETETLEN": return "F";
            default: return "SZ";
        }
    }

    /**
     * Visszaadja a brigád eltolási értékét (offset), ami garantálja, hogy
     * minden napra pontosan 1 nappalos és 1 éjszakás brigád jut: a brigádok
     * egy-egy nap késéssel követik egymást (A, B, C, D sorrendben kezdik a nappalt).
     */
    public static int getTeamOffset(int teamId) {
        switch (teamId) {
            case 1: return 0; // "A" Brigád
            case 2: return 3; // "B" Brigád
            case 3: return 2; // "C" Brigád
            case 4: return 1; // "D" Brigád
            default: return 0;
        }
    }

    public static String getShiftCodeForDay(int teamId, LocalDate date) {
        long days = ChronoUnit.DAYS.between(CYCLE_EPOCH, date);
        int offset = getTeamOffset(teamId);
        int len = BASE_CYCLE.length;
        int cycleIndex = (int) (((days + offset) % len + len) % len);
        return BASE_CYCLE[cycleIndex];
    }
}
