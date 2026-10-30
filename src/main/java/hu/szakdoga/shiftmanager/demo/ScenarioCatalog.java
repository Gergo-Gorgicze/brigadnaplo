package hu.szakdoga.shiftmanager.demo;

import hu.szakdoga.shiftmanager.demo.scenarios.*;
import hu.szakdoga.shiftmanager.model.ShiftType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;

/**
 * A választható demó helyzetek listája.
 */
public final class ScenarioCatalog {

    private static final List<DemoScenario> SCENARIOS = Collections.unmodifiableList(Arrays.asList(
            new BaseScenario(),
            new Scenario1AccumulatedLeave(),
            new Scenario2FluSeason(),
            new Scenario3RestPeriod(),
            new Scenario4HourBalance(),
            new Scenario5Christmas()
    ));

    private ScenarioCatalog() {}

    public static List<DemoScenario> all() {
        return SCENARIOS;
    }

    public static DemoScenario byId(String id) {
        if (id == null) return null;
        for (DemoScenario s : SCENARIOS) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    /**
     * A séma szerinti műszaktípusok (azonos azonosítókkal), adatbázis nélküli használatra (tesztek).
     */
    public static Map<String, ShiftType> standardShiftTypes() {
        Map<String, ShiftType> map = new LinkedHashMap<>();
        LocalTime zero = LocalTime.MIDNIGHT;
        map.put("N", new ShiftType(1, "Nappal (06-18)", "N", LocalTime.of(6, 0), LocalTime.of(18, 0), "#F57C00", true, 45));
        map.put("É", new ShiftType(2, "Éjszaka (18-06)", "É", LocalTime.of(18, 0), LocalTime.of(6, 0), "#1976D2", true, 45));
        map.put("P", new ShiftType(3, "Pihenőnap", "P", zero, zero, "#4CAF50", false, 0));
        map.put("SZ", new ShiftType(4, "Szabadság", "SZ", zero, zero, "#9C27B0", false, 0));
        map.put("B", new ShiftType(5, "Betegszabadság / Táppénz", "B", zero, zero, "#E53935", false, 0));
        map.put("F", new ShiftType(6, "Fizetés nélküli szabadság", "F", zero, zero, "#757575", false, 0));
        return map;
    }

    /**
     * Alapállapot: a 24 fős üzem 2026-os éve a szokásos (tervezett) szabadságokkal, különleges esemény nélkül.
     * Az alkalmazás első indításkor ezt tölti be.
     */
    public static class BaseScenario extends DemoScenario {
        @Override public String getId() { return "0"; }
        @Override public String getTitle() { return "Alapállapot – a 2026-os üzemi év"; }
        @Override public String getShortDescription() {
            return "A 4 brigád 24 dolgozója a teljes 2026-os évre generált N–É–P–P (nappal–éjszaka–pihenő–pihenő) beosztással és a tervezett szabadságokkal. Ebből indul a program első indításkor.";
        }
        @Override public LocalDate getSimulatedToday() { return LocalDate.of(2026, 6, 1); }
        @Override public YearMonth getFocusMonth() { return YearMonth.of(2026, 6); }
        @Override public String getStory() {
            return "Egy folyamatos üzemű műanyag-feldolgozó üzem 4 brigádja, brigádonként 6 gépkezelővel. A beosztás "
                    + "az N–É–P–P rotáció szerint az egész évre elkészült, a dolgozók éves szabadságolási terve (tavaszi, nyári "
                    + "és őszi szabadságok) rögzítve van, és az év első felében néhány rövid megbetegedés történt. "
                    + "Ebben az állapotban a program bármely funkciója szabadon kipróbálható.";
        }
        @Override public List<String> getSteps() {
            return Arrays.asList(
                    "A beosztás nézetben lapozz a hónapok között, és figyeld meg az N–É–P–P rotációt és a szabadságokat.",
                    "Az „Éves nyilvántartás” menüben nézd meg a dolgozók szabadságkeretét és órakontóját.",
                    "Rögzíts egy új távollétet, vagy módosíts kézzel egy műszakot, és mentsd el a beosztást.");
        }
        @Override public String getTakeaway() {
            return "A program alapműködése: törvényes szabadságkeret, N–É–P–P vezénylés, órakontó és pótlékos órák.";
        }
        @Override public void define(ScenarioBuilder builder) {
            DemoRoster.addStandardRoster(builder);
        }
    }
}
