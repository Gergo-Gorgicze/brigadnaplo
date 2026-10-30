package hu.szakdoga.shiftmanager.service;

import hu.szakdoga.shiftmanager.dao.ScheduleDAO;
import hu.szakdoga.shiftmanager.dao.ShiftTypeDAO;
import hu.szakdoga.shiftmanager.dao.TimeOffDAO;
import hu.szakdoga.shiftmanager.dao.UserDAO;
import hu.szakdoga.shiftmanager.model.*;
import hu.szakdoga.shiftmanager.util.AppClock;
import hu.szakdoga.shiftmanager.util.HungarianCalendar;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Éves Óranyilvántartó és Szabadságkeret Menedzsment Szolgáltatás.
 * A Munka Törvénykönyve (Mt. 115-126. §) előírásai szerint kezeli a dolgozók
 * éves alapszabadságát, életkori és családi pótszabadságát, a 15 napos betegszabadságot,
 * a táppénzes napokat, valamint a munkaidőkeret éves órakontóját (+ / - órák).
 *
 * A távollétek hosszát munkanapokban (hétfő–péntek, munkaszüneti napok nélkül) számoljuk.
 * A szabadság és a betegség idejére a dolgozó a beosztása szerint kieső műszak óráit
 * jóváírásként megkapja, így az órakontó csak a valódi többlet- vagy hiányórákat mutatja
 * (pl. bevállalt plusz műszak, fizetés nélküli szabadság).
 */
public class AnnualTimeTrackingService {
    private final UserDAO userDAO = new UserDAO();
    private final TimeOffDAO timeOffDAO = new TimeOffDAO();
    private final ScheduleDAO scheduleDAO = new ScheduleDAO();
    private final ShiftTypeDAO shiftTypeDAO = new ShiftTypeDAO();
    private final StaffingService staffingService = new StaffingService();

    /** Alapértelmezett nettó műszakhossz (12 óra - 45 perc szünet), ha az adatbázisból nem olvasható. */
    private static final double DEFAULT_SHIFT_NET_HOURS = 11.25;

    public static class LeaveValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> conflicts = new ArrayList<>();

        /** Érvényes, ha nincs olyan hiba, amely miatt a kérelem semmiképp nem rögzíthető. */
        public boolean isValid() { return errors.isEmpty(); }
        /** Kemény hibák (pl. nincs elég keret, átfedés) – a kérelem nem rögzíthető. */
        public List<String> getErrors() { return errors; }
        /** Figyelmeztetések – a műszakvezető tudomásul veszi és dönt. */
        public List<String> getWarnings() { return warnings; }
        /** Létszámütközések – elutasítási ok, de a műszakvezető indokolt esetben felülbírálhatja. */
        public List<String> getConflicts() { return conflicts; }
        public boolean hasConflicts() { return !conflicts.isEmpty(); }

        public String getSummaryMessage() {
            StringBuilder sb = new StringBuilder();
            if (!errors.isEmpty()) {
                sb.append("HIBA:\n").append(String.join("\n", errors)).append("\n\n");
            }
            if (!conflicts.isEmpty()) {
                sb.append("LÉTSZÁMHIÁNY:\n").append(String.join("\n", conflicts)).append("\n\n");
            }
            if (!warnings.isEmpty()) {
                sb.append("FIGYELMEZTETÉS:\n").append(String.join("\n", warnings));
            }
            return sb.toString().trim();
        }
    }

    /**
     * Kiszámítja az összes dolgozó éves szabadság- és óraegyenlegét a megadott évre.
     */
    public List<AnnualLeaveBalance> calculateAnnualBalances(int year) {
        List<AnnualLeaveBalance> balances = new ArrayList<>();
        List<User> users = userDAO.getAllUsers();
        List<TimeOffRequest> requests = timeOffDAO.getForYear(year);

        // Az év összes beosztását egyszer töltjük be, és dolgozónként csoportosítjuk
        Map<Integer, List<WorkSchedule>> schedulesByUser = new HashMap<>();
        for (WorkSchedule ws : scheduleDAO.getSchedulesForYear(year)) {
            schedulesByUser.computeIfAbsent(ws.getUser().getId(), k -> new ArrayList<>()).add(ws);
        }

        double shiftNetHours = DEFAULT_SHIFT_NET_HOURS;
        ShiftType dayShift = shiftTypeDAO.getShiftTypesByCodeMap().get("N");
        if (dayShift != null) shiftNetHours = dayShift.getNetWorkingHours();

        double annualNormativeHours = calculateAnnualNormativeHours(year);

        for (User user : users) {
            balances.add(computeBalance(user, year, requests,
                    schedulesByUser.getOrDefault(user.getId(), Collections.emptyList()),
                    annualNormativeHours, shiftNetHours));
        }
        return balances;
    }

    /**
     * Egy dolgozó éves egyenlegének tiszta (adatbázistól független) kiszámítása.
     *
     * @param requests          az év távolléti kérelmei (bármely dolgozóé, csak a jóváhagyottak számítanak)
     * @param userYearSchedules a dolgozó adott évi beosztásai
     */
    public static AnnualLeaveBalance computeBalance(User user, int year, List<TimeOffRequest> requests,
                                                    List<WorkSchedule> userYearSchedules,
                                                    double annualNormativeHours, double shiftNetHours) {
        AnnualLeaveBalance bal = new AnnualLeaveBalance(user, year);

        // 1. Törvényes szabadságkeret számítása (Mt. 115-120. §)
        int totalVacDays = user.calculateTotalAnnualVacationDays(year);
        bal.setTotalVacationDays(totalVacDays);
        bal.setTotalVacationHours(totalVacDays * 8.0);
        bal.setNormativeHours(annualNormativeHours);

        // 2. Távollétek összegzése (jóváhagyott kérelmek alapján, munkanapokban)
        int usedVacDays = 0;
        int totalSickDays = 0;
        int unpaidDays = 0;
        double absenceCredit = 0;

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        for (TimeOffRequest req : requests) {
            if (req.getUser() == null || req.getUser().getId() != user.getId()) {
                continue;
            }
            LocalDate start = req.getStartDate().isBefore(yearStart) ? yearStart : req.getStartDate();
            LocalDate end = req.getEndDate().isAfter(yearEnd) ? yearEnd : req.getEndDate();
            if (start.isAfter(end)) continue;

            int days = HungarianCalendar.countWorkdays(start, end);
            String type = req.getType() == null ? "" : req.getType().toUpperCase();

            switch (type) {
                case "SZABADSAG":
                    usedVacDays += days;
                    break;
                case "BETEGSZABADSAG":
                    totalSickDays += days;
                    break;
                case "FIZETETLEN":
                    unpaidDays += days;
                    break;
            }

            // A szabadság és a betegség idejére a kieső beosztás szerinti műszakórákat jóváírjuk
            // (a fizetés nélküli szabadság nem számít bele a teljesített munkaidőbe).
            if (("SZABADSAG".equals(type) || "BETEGSZABADSAG".equals(type)) && user.getTeam() != null) {
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    String code = AutoSchedulerService.getShiftCodeForDay(user.getTeam().getId(), d);
                    if ("N".equals(code) || "É".equals(code)) {
                        absenceCredit += shiftNetHours;
                    }
                }
            }
        }

        bal.setUsedVacationDays(usedVacDays);
        bal.setUsedVacationHours(usedVacDays * 8.0);

        int remainingVacDays = totalVacDays - usedVacDays;
        bal.setRemainingVacationDays(remainingVacDays);
        bal.setRemainingVacationHours(remainingVacDays * 8.0);

        // Betegszabadság (évi 15 munkanapig a munkáltató terhére - Mt. 126. §) vs. Táppénz (15 napon túl)
        if (totalSickDays <= 15) {
            bal.setSickLeaveDays(totalSickDays);
            bal.setSickPayDays(0);
        } else {
            bal.setSickLeaveDays(15);
            bal.setSickPayDays(totalSickDays - 15);
        }
        bal.setUnpaidLeaveDays(unpaidDays);

        // 3. Beosztott munkaórák és munkaidőkeret-egyenleg (órakontó)
        double scheduledHours = 0;
        for (WorkSchedule ws : userYearSchedules) {
            if (ws.getShiftType() != null && ws.getShiftType().isWorkingShift()) {
                scheduledHours += ws.getShiftType().getNetWorkingHours();
            }
        }
        bal.setScheduledHours(scheduledHours);
        bal.setAbsenceCreditHours(absenceCredit);
        bal.setHourBalance(scheduledHours + absenceCredit - annualNormativeHours);

        return bal;
    }

    /**
     * Szabadságigény előzetes validálása a Munka Törvénykönyve szabályai és a műszaklétszám szerint.
     */
    public LeaveValidationResult validateAndCheckLeaveRequest(User user, LocalDate start, LocalDate end, String type, int year) {
        LeaveValidationResult result = new LeaveValidationResult();

        if (start == null || end == null) {
            result.getErrors().add("A kezdő és záró dátum megadása kötelező!");
            return result;
        }

        if (start.isAfter(end)) {
            result.getErrors().add("A kezdő dátum nem lehet későbbi a záró dátumnál!");
            return result;
        }

        int requestedDays = HungarianCalendar.countWorkdays(start, end);

        // Átfedés egy már jóváhagyott távolléttel
        for (TimeOffRequest r : timeOffDAO.getForPeriod(start, end)) {
            if (r.getUser() != null && r.getUser().getId() == user.getId()) {
                result.getErrors().add(String.format(
                        "Átfedés: %s és %s között már rögzített távolléte van (%s).",
                        r.getStartDate(), r.getEndDate(), r.getType()));
                return result;
            }
        }

        List<AnnualLeaveBalance> balances = calculateAnnualBalances(year);
        AnnualLeaveBalance userBal = balances.stream()
                .filter(b -> b.getUser().getId() == user.getId())
                .findFirst()
                .orElse(null);

        // Szabadságkeret ellenőrzése
        if ("SZABADSAG".equalsIgnoreCase(type)) {
            if (requestedDays == 0) {
                result.getErrors().add("A megadott időszak nem tartalmaz munkanapot (hétfő–péntek), így szabadságnap nem számolható el rá.");
                return result;
            }

            if (userBal != null && requestedDays > userBal.getRemainingVacationDays()) {
                result.getErrors().add(String.format(
                        "Mt. 115-120. §: Nincs elegendő szabadságkeret! Igényelt: %d nap, még kivehető: %d nap (Éves keret: %d nap).",
                        requestedDays, userBal.getRemainingVacationDays(), userBal.getTotalVacationDays()
                ));
                return result;
            }

            // 15 napos előzetes bejelentési szabály (Mt. 122. §)
            long daysUntilStart = ChronoUnit.DAYS.between(AppClock.today(), start);
            if (daysUntilStart < 15) {
                result.getWarnings().add(String.format(
                        "Mt. 122. § Figyelmeztetés: A szabadság időpontját legalább 15 nappal a kezdete előtt közölni kell! (Hátralévő napok: %d).",
                        daysUntilStart
                ));
            }
        }

        // Betegszabadság táppénz túllépési figyelmeztetés (Mt. 126. §)
        if ("BETEGSZABADSAG".equalsIgnoreCase(type) && userBal != null) {
            int currentSick = userBal.getSickLeaveDays() + userBal.getSickPayDays();
            if (currentSick + requestedDays > 15) {
                int sickPayDays = Math.min(requestedDays, currentSick + requestedDays - 15);
                result.getWarnings().add(String.format(
                        "Mt. 126. § Figyelmeztetés: A dolgozó idén már %d munkanapot volt beteg. Az évi 15 munkanap betegszabadság felett a mostani távollétből %d munkanap táppénzes állományba kerül.",
                        currentSick, sickPayDays
                ));
            }
        }

        // Műszaklétszám ellenőrzése: hány fő maradna a dolgozó műszakjában?
        List<StaffingService.Shortage> shortages = staffingService.checkLeaveImpact(user, start, end);
        if (!shortages.isEmpty()) {
            String brigade = user.getTeam() != null ? user.getTeam().getName() : "brigád";
            String list = StaffingService.format(shortages, 12);
            if ("BETEGSZABADSAG".equalsIgnoreCase(type)) {
                result.getWarnings().add("Létszámhiány! A keresőképtelenség nem utasítható el, de a(z) " + brigade +
                        " alábbi műszakjai a minimális létszám alá esnek – helyettesítést kell szervezni:\n" + list);
            } else {
                result.getConflicts().add("Ha a kérelmet jóváhagyjuk, a(z) " + brigade +
                        " alábbi műszakjai a minimális létszám alá esnének:\n" + list +
                        "\nA szabadság kiadásának időpontját a munkáltató határozza meg (Mt. 122. §), " +
                        "ezért a kérelem ebben az időszakban üzemi okból elutasítható. Kivétel: az évi 7 munkanap, " +
                        "amelyet a munkavállaló kérésére kell kiadni (Mt. 122. § (2)) – ilyenkor helyettesítést kell szervezni.");
            }
        }

        return result;
    }

    /**
     * Kiszámítja az év munkanapjai alapján a törvényes munkaóra normát (hétköznapok minusz ünnepnapok * 8 óra).
     */
    public static double calculateAnnualNormativeHours(int year) {
        return HungarianCalendar.countWorkdays(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)) * 8.0;
    }
}
