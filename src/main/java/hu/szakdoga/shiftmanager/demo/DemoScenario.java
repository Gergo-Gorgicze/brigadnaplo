package hu.szakdoga.shiftmanager.demo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Egy bemutatható, életszerű helyzet: történet, szereplők, a betöltendő adatok és a bemutatás lépései.
 */
public abstract class DemoScenario {

    private static final DateTimeFormatter HU_DATE = DateTimeFormatter.ofPattern("yyyy. MMMM d. (EEEE)", new Locale("hu", "HU"));

    /** Rövid azonosító (pl. "1"), az adatbázisban is ezt tároljuk. */
    public abstract String getId();

    public abstract String getTitle();

    /** Egy-két mondatos összefoglaló a listában. */
    public abstract String getShortDescription();

    /** A helyzet "mai napja" – a program ezt tekinti aktuális dátumnak. */
    public abstract LocalDate getSimulatedToday();

    /** A hónap, amelyet a beosztás nézet betöltés után megnyit. */
    public abstract YearMonth getFocusMonth();

    /** A helyzet háttértörténete (több bekezdés). */
    public abstract String getStory();

    /** A bemutatás lépései a programban. */
    public abstract List<String> getSteps();

    /** Mit bizonyít a helyzet: mely programlogika és jogszabály kerül elő. */
    public abstract String getTakeaway();

    /** A helyzet adatai (dolgozók, távollétek, plusz műszakok). */
    public abstract void define(ScenarioBuilder builder);

    /** Minimális létszám műszakonként (gépkezelők + beállító). */
    public int getMinCrewPerShift() {
        return 4;
    }

    public String getDisplayName() {
        return getId() + ". " + getTitle();
    }

    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTitle().toUpperCase(new Locale("hu", "HU"))).append("\n\n");
        sb.append("Szimulált mai nap: ").append(getSimulatedToday().format(HU_DATE)).append('\n');
        sb.append("Minimális létszám: ").append(getMinCrewPerShift()).append(" fő / műszak (brigádonként 6 gépkezelő)\n\n");
        sb.append("A HELYZET\n").append(getStory().trim()).append("\n\n");
        sb.append("BEMUTATÁS LÉPÉSEI\n");
        int i = 1;
        for (String step : getSteps()) {
            sb.append(i++).append(". ").append(step).append('\n');
        }
        sb.append("\nMIT MUTAT MEG A PROGRAM?\n").append(getTakeaway().trim()).append('\n');
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
