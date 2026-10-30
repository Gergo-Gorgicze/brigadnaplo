package hu.szakdoga.shiftmanager.demo.scenarios;

import hu.szakdoga.shiftmanager.demo.DemoScenario;
import hu.szakdoga.shiftmanager.demo.ScenarioBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static hu.szakdoga.shiftmanager.demo.DemoRoster.*;

public class Scenario2FluSeason extends DemoScenario {
    @Override public String getId() { return "2"; }
    @Override public String getTitle() { return "Influenzaszezon a B brigádban – a betegség nem utasítható el"; }
    @Override public String getShortDescription() {
        return "Februári influenzajárvány: a B brigádból már ketten betegek, ma reggel Balogh Ádám is lebetegszik. A betegség nem utasítható el, de a rendszer jelzi a táppénzt és a létszámhiányt.";
    }
    @Override public LocalDate getSimulatedToday() { return LocalDate.of(2026, 2, 9); }
    @Override public YearMonth getFocusMonth() { return YearMonth.of(2026, 2); }
    @Override public String getStory() {
        return """
                Február eleje, influenzaszezon. A B brigádban Papp Rita február 5. óta, Szűcs Dániel február 6. óta
                beteg (mindkettőjüket február 13-ig írta ki az orvos). A brigád így 4 fővel – éppen a minimummal – dolgozik.

                Ma, február 9-én (hétfő) reggel Balogh Ádám is telefonál: lázas, az orvos február 18-ig keresőképtelennek
                írta ki. Ádám januárban már három hétig (január 12–30., 15 munkanap) gerincsérv miatt volt betegállományban,
                vagyis az idei 15 munkanap betegszabadságát már teljesen felhasználta.

                A B brigád ma pihenőnapos, holnap (február 10.) nappalos (06:00–18:00), szerdán pedig éjszakás
                (18:00–06:00) műszakban dolgozik. Ádám kiesésével mindkét műszakban csak 3 fő maradna.
                A keresőképtelenséget nem lehet elutasítani – a műszakvezetőnek viszont azonnal tudnia kell, hogy:
                mely műszakok lettek hiányosak, kit lehet szabályosan beosztani helyettesnek, és hogy Ádám távolléte
                már táppénznek számít-e.
                """;
    }
    @Override public List<String> getSteps() {
        return Arrays.asList(
                "Töltsd be a helyzetet. A beosztás nézet 2026. februárt mutatja: a B brigádban Papp Rita és Szűcs Dániel napjain piros [B] látható.",
                "„👥 Éves nyilvántartás”: Balogh Ádám betegszabadsága 15 / 15 nap – az idei keret elfogyott.",
                "Az „Éves nyilvántartás” ablakban „➕ Új Távollét”: Balogh Ádám, típus: BETEGSZABADSAG, 2026.02.09 – 2026.02.18, indoklás: „Influenza (orvosi igazolás)”.",
                "A rendszer nem tiltja le a rögzítést, de két figyelmeztetést ad: (1) Mt. 126. §: idén már 15 munkanapot volt beteg, ezért a mostani 8 munkanap teljes egészében táppénz; (2) Létszámhiány: február 10. nappalos és február 11. éjszakás műszakjában csak 3 fő marad. Kattints az „OK” gombra.",
                "Az „Éves nyilvántartás” táblázatában Ádám táppénz oszlopa 8 napra nő (piros).",
                "A beosztás nézetben (február) Ádám napjai már [B]-vel jelennek meg. Az „⚡ Automatikus Tervezés” gomb ellenőrzi a havi tervet, és kilistázza a két hiányos műszakot.",
                "Helyettesítés: jobb klikk Rácz Dávid (D brigád, február 10-én pihenőnapos) február 10-i celláján → „Nappal (06-18) [N]”, majd kattints a „💾 Mentés” gombra.",
                "A mentés megtagadva: a D brigád február 9-én éjszakás volt, Rácz Dávid tehát február 10-én reggel 6-kor jönne le a gépről, és ugyanakkor kellene újrakezdenie – 0 óra pihenő (Mt. 104. §). Állítsd vissza a cellát „Pihenőnap [P]”-re. A február 10-i nappalt Lakatos Zsófia (C brigád, február 9–10. pihenőnapos), a február 11-i éjszakát Vass Levente (A brigád, február 11–12. pihenőnapos) kapja.",
                "Mentés újra: most már sikeres, és nem marad hiányos műszak. Az „Éves nyilvántartás” táblázatában Lakatos Zsófia és Vass Levente órakontója +11,25 órával nő."
        );
    }
    @Override public String getTakeaway() {
        return """
                • A keresőképtelenség (BETEGSZABADSAG) sosem utasítható el – a program ezért csak figyelmeztet, nem tilt.
                • Mt. 126. §: évi 15 munkanap betegszabadság, felette táppénz – a rendszer napra pontosan kiszámolja, mennyi
                  a táppénzes rész (ez a bérszámfejtés szempontjából lényeges).
                • Létszámfigyelés: a brigád minden érintett műszakjáról azonnal kiderül, hogy a minimum alá esett.
                • Helyettesítés szervezése: a mentés előtti munkajogi ellenőrzés (Mt. 104. §, 11 óra pihenőidő) kiszűri
                  a jó szándékú, de szabálytalan beosztást (éjszaka után másnap reggeli kezdés).
                """;
    }

    @Override
    public void define(ScenarioBuilder b) {
        addStandardRoster(b);
        b.sick(BALOGH, d(1, 12), d(1, 30), "Gerincsérv, orvosi igazolással");
        b.sick(PAPP, d(2, 5), d(2, 13), "Influenza");
        b.sick(SZUCS, d(2, 6), d(2, 13), "Influenza");
    }

    private static LocalDate d(int month, int day) { return LocalDate.of(YEAR, month, day); }
}
