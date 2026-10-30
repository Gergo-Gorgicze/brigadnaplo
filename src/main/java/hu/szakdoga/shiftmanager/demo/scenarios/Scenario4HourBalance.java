package hu.szakdoga.shiftmanager.demo.scenarios;

import hu.szakdoga.shiftmanager.demo.DemoScenario;
import hu.szakdoga.shiftmanager.demo.ScenarioBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static hu.szakdoga.shiftmanager.demo.DemoRoster.*;

public class Scenario4HourBalance extends DemoScenario {
    @Override public String getId() { return "4"; }
    @Override public String getTitle() { return "Órakontó – ki dolgozott túl sokat, és kinek van mínusza?"; }
    @Override public String getShortDescription() {
        return "Szeptember végén az éves órakontó megmutatja, hogy Horváth Richárd egész nyáron túlórázott (+100,75 óra), Takács Péter viszont fizetés nélküli szabadsága miatt mínuszban van.";
    }
    @Override public LocalDate getSimulatedToday() { return LocalDate.of(2026, 10, 1); }
    @Override public YearMonth getFocusMonth() { return YearMonth.of(2026, 8); }
    @Override public String getStory() {
        return """
                Október 1. – a nyári szabadságolási időszak lezárult. A nyár folyamán a D brigádos Horváth Richárd
                (35 éves, egyedülálló) szinte minden felkérésre igent mondott: június 27. és szeptember 3. között 9 alkalommal
                jött be a pihenőnapján helyettesíteni.
                Kiss Nóra (A brigád) 3 plusz műszakot vállalt.

                Takács Péter (D brigád) július 6–24. között családi okból – megállapodás alapján – fizetés nélküli
                szabadságon volt (15 munkanap). Erre az időre nem kap munkabért, és ezek az órák a munkaidőkeretből is hiányoznak.

                A műszakvezető előtt két kérdés áll:
                 1. Kinek adja a következő plusz műszakot úgy, hogy a terhelés igazságos legyen?
                 2. Nem fut-e ki valaki az éves túlóra-keretből (Mt. 109. §: évi legfeljebb 250 óra rendkívüli munkaidő)?
                Papíron vagy Excelben ezt csak hosszas utólagos számolással lehetne megválaszolni.
                """;
    }
    @Override public List<String> getSteps() {
        return Arrays.asList(
                "Töltsd be a helyzetet. A beosztás nézet 2026. augusztust mutatja: Horváth Richárd sorában a pihenőnapokon is narancs [N] műszakok láthatók (aug. 2., 10., 18., 26.). (Ebben a helyzetben NE nyomd meg az „Automatikus Tervezés” gombot, mert az a plusz műszakokat felülírná a rácsban.)",
                "Lépj vissza júliusra (◀): Takács Péter sorában július 6–24. között szürke [F] (fizetés nélküli szabadság) látható.",
                "„👥 Éves nyilvántartás”: kattints a „Keretegyenleg (Órakontó)” oszlop fejlécére a rendezéshez. Legfelül Horváth Richárd +100,75 h, legalul Takács Péter −113,0 h (15 nap fizetés nélküli szabadság).",
                "Figyeld meg a brigádok alapértékét: az N–É–P–P rotáció fázisa miatt egy teljes évben az A és a B brigád +10,75 h, a C és a D brigád −0,5 h egyenleggel zárna. Ezt a munkaidőkeret végén kell kiegyenlíteni.",
                "„📊 Havi pótlékok” (augusztus): a havi beosztott órák mellett külön látszanak a 30%-os műszakpótlékos (éjszakai), a vasárnapi és a munkaszüneti napi (augusztus 20., 100% pótlék) órák.",
                "Döntés: a következő plusz műszakot ne Richárd kapja (túlterhelés, közelít az éves túlóra-kerethez), hanem egy negatív egyenlegű B brigádos dolgozó – vagy, ha vállalja, Takács Péter, aki így a mínuszát dolgozhatja le."
        );
    }
    @Override public String getTakeaway() {
        return """
                • Órakontó (munkaidőkeret egyenlege) = beosztott órák + szabadság/betegség miatt kiesett műszakórák jóváírása
                  − éves norma (munkanapok × 8 óra). A szabadság és a betegség nem rontja az egyenleget, a fizetés nélküli
                  szabadság igen – pontosan úgy, ahogy a bérszámfejtésben is.
                • A plusz műszakok automatikusan megjelennek az egyenlegben: azonnal látszik, ki dolgozik túl sokat
                  (Mt. 109. §: évi 250 óra rendkívüli munkaidő), és ki van lemaradásban.
                • A pótlékalapok (Mt. 140–141. §: éjszakai műszakpótlék, vasárnap, munkaszüneti nap) havonta összesíthetők.
                • Átlátható, igazságos túlóra-elosztás: az N–É–P–P rotáció brigádonkénti eltérő éves egyenlege is látható,
                  ami Excelben gyakori vita forrása.
                """;
    }

    @Override
    public void define(ScenarioBuilder b) {
        addStandardRoster(b);
        for (LocalDate extra : new LocalDate[]{d(6, 27), d(7, 9), d(7, 17), d(7, 25), d(8, 2),
                d(8, 10), d(8, 18), d(8, 26), d(9, 3)}) {
            b.extraShift(HORVATH, extra, "N", "Plusz műszak – nyári szabadságok helyettesítése");
        }
        for (LocalDate extra : new LocalDate[]{d(7, 14), d(8, 7), d(9, 8)}) {
            b.extraShift(KISS, extra, "N", "Plusz műszak – helyettesítés");
        }
        b.unpaid(TAKACS, d(7, 6), d(7, 24), "Családi ok – fizetés nélküli szabadság (megállapodás alapján)");
    }

    private static LocalDate d(int month, int day) { return LocalDate.of(YEAR, month, day); }
}
