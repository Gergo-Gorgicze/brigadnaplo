package hu.szakdoga.shiftmanager.demo;

import hu.szakdoga.shiftmanager.model.*;
import hu.szakdoga.shiftmanager.service.AutoSchedulerService;

import java.time.LocalDate;
import java.util.*;

/**
 * Egy demó helyzet adatainak összeállítása memóriában: dolgozók, távollétek, plusz műszakok.
 *
 * A {@link #materialize} metódus ebből elkészíti a kész modellobjektumokat
 * (User, TimeOffRequest, egész éves WorkSchedule), amelyeket a {@link ScenarioLoader}
 * ír ki az adatbázisba, az egységtesztek pedig közvetlenül ellenőriznek.
 */
public class ScenarioBuilder {

    public static final String SZABADSAG = "SZABADSAG";
    public static final String BETEG = "BETEGSZABADSAG";
    public static final String FIZETETLEN = "FIZETETLEN";

    private static final Map<Integer, Team> TEAMS = new HashMap<>();
    static {
        TEAMS.put(1, new Team(1, "A Brigád", "1. számú folyamatos termelési brigád"));
        TEAMS.put(2, new Team(2, "B Brigád", "2. számú folyamatos termelési brigád"));
        TEAMS.put(3, new Team(3, "C Brigád", "3. számú folyamatos termelési brigád"));
        TEAMS.put(4, new Team(4, "D Brigád", "4. számú folyamatos termelési brigád"));
    }

    public static class Person {
        public final String key;
        public final String lastName;
        public final String firstName;
        public final int teamId;
        public final LocalDate birthDate;
        public final LocalDate contractStart;
        public final int children;
        public final boolean disability;

        Person(String key, String lastName, String firstName, int teamId, LocalDate birthDate,
               LocalDate contractStart, int children, boolean disability) {
            this.key = key;
            this.lastName = lastName;
            this.firstName = firstName;
            this.teamId = teamId;
            this.birthDate = birthDate;
            this.contractStart = contractStart;
            this.children = children;
            this.disability = disability;
        }

    }

    public static class Leave {
        public final String personKey;
        public final LocalDate start;
        public final LocalDate end;
        public final String type;
        public final String reason;

        Leave(String personKey, LocalDate start, LocalDate end, String type, String reason) {
            this.personKey = personKey;
            this.start = start;
            this.end = end;
            this.type = type;
            this.reason = reason;
        }
    }

    public static class ExtraShift {
        public final String personKey;
        public final LocalDate date;
        public final String code;
        public final String note;

        ExtraShift(String personKey, LocalDate date, String code, String note) {
            this.personKey = personKey;
            this.date = date;
            this.code = code;
            this.note = note;
        }
    }

    /** A kész, adatbázisba írható / tesztelhető adatcsomag. */
    public static class Materialized {
        public final List<User> users = new ArrayList<>();
        public final Map<String, User> usersByKey = new LinkedHashMap<>();
        public final List<TimeOffRequest> requests = new ArrayList<>();
        public final List<WorkSchedule> schedules = new ArrayList<>();
        public final List<ExtraShift> extraShifts = new ArrayList<>();

        public User user(String key) {
            User u = usersByKey.get(key);
            if (u == null) throw new IllegalArgumentException("Ismeretlen dolgozó: " + key);
            return u;
        }
    }

    private final Map<String, Person> people = new LinkedHashMap<>();
    private final List<Leave> leaves = new ArrayList<>();
    private final List<ExtraShift> extras = new ArrayList<>();

    public ScenarioBuilder person(String key, String lastName, String firstName, int teamId,
                                  LocalDate birthDate, LocalDate contractStart, int children, boolean disability) {
        people.put(key, new Person(key, lastName, firstName, teamId, birthDate, contractStart, children, disability));
        return this;
    }

    public ScenarioBuilder vacation(String key, LocalDate start, LocalDate end, String reason) {
        return leave(key, start, end, SZABADSAG, reason);
    }

    public ScenarioBuilder sick(String key, LocalDate start, LocalDate end, String reason) {
        return leave(key, start, end, BETEG, reason);
    }

    public ScenarioBuilder unpaid(String key, LocalDate start, LocalDate end, String reason) {
        return leave(key, start, end, FIZETETLEN, reason);
    }

    public ScenarioBuilder leave(String key, LocalDate start, LocalDate end, String type, String reason) {
        requirePerson(key);
        leaves.add(new Leave(key, start, end, type, reason));
        return this;
    }

    /** Plusz (helyettesítő) műszak egy dolgozó egyébként pihenőnapjára. */
    public ScenarioBuilder extraShift(String key, LocalDate date, String code, String note) {
        requirePerson(key);
        extras.add(new ExtraShift(key, date, code, note));
        return this;
    }

    /** Egy dolgozó adott típusú távolléteinek törlése (pl. a háttéradatok felülírásához). */
    public ScenarioBuilder removeLeaves(String key, String type) {
        leaves.removeIf(l -> l.personKey.equals(key) && l.type.equals(type));
        return this;
    }

    public Collection<Person> getPeople() { return people.values(); }
    public List<Leave> getLeaves() { return leaves; }
    public List<ExtraShift> getExtras() { return extras; }

    private void requirePerson(String key) {
        if (!people.containsKey(key)) {
            throw new IllegalArgumentException("Ismeretlen dolgozó a demó adatokban: " + key);
        }
    }

    /**
     * Elkészíti a helyzet teljes adatkészletét.
     *
     * @param today    a szimulált "mai nap" – a jövőbeli betegségek kimaradnak (azt senki nem tudhatja előre),
     *                 a jövőbeli szabadságok viszont szerepelnek (az éves szabadságolási terv része)
     * @param year     az év, amelyre a teljes beosztás elkészül
     * @param shiftMap műszaktípusok kód szerint (N, É, P, SZ, B, F)
     */
    public Materialized materialize(LocalDate today, int year, Map<String, ShiftType> shiftMap) {
        Materialized m = new Materialized();
        int id = 1;
        for (Person p : people.values()) {
            User u = new User(id++, p.firstName, p.lastName, TEAMS.get(p.teamId),
                    p.contractStart, p.birthDate, p.children, p.disability);
            m.users.add(u);
            m.usersByKey.put(p.key, u);
        }

        int reqId = 1;
        for (Leave l : leaves) {
            if (BETEG.equals(l.type) && l.start.isAfter(today)) {
                continue; // jövőbeli betegség nem lehet az adatbázisban
            }
            m.requests.add(new TimeOffRequest(reqId++, m.user(l.personKey), l.start, l.end, l.type, l.reason));
        }

        for (int month = 1; month <= 12; month++) {
            m.schedules.addAll(AutoSchedulerService.buildMonthlySchedule(m.users, m.requests, shiftMap, year, month));
        }

        for (ExtraShift e : extras) {
            if (e.date.getYear() != year) continue;
            User u = m.user(e.personKey);
            for (WorkSchedule ws : m.schedules) {
                if (ws.getUser().getId() == u.getId() && ws.getDate().equals(e.date)) {
                    ws.setShiftType(shiftMap.get(e.code));
                    ws.setNotes(e.note);
                }
            }
            m.extraShifts.add(e);
        }
        return m;
    }
}
