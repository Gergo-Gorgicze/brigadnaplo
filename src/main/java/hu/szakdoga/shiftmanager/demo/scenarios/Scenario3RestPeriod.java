package hu.szakdoga.shiftmanager.demo.scenarios;

import hu.szakdoga.shiftmanager.demo.DemoScenario;
import hu.szakdoga.shiftmanager.demo.ScenarioBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static hu.szakdoga.shiftmanager.demo.DemoRoster.*;

public class Scenario3RestPeriod extends DemoScenario {
    @Override public String getId() { return "3"; }
    @Override public String getTitle() { return "Éjszaka után reggel? – a 11 órás pihenőidő védelme"; }
    @Override public String getShortDescription() {
        return "Vasárnap este betelefonál egy beteg dolgozó, hétfő reggelre helyettes kell. A kézenfekvő megoldás (aki most jön le éjszakából) szabálytalan – a program nem engedi menteni.";
    }
    @Override public LocalDate getSimulatedToday() { return LocalDate.of(2026, 3, 8); }
    @Override public YearMonth getFocusMonth() { return YearMonth.of(2026, 3); }
    @Override public String getStory() {
        return """
                Március 8., vasárnap este 20:00. Az A brigád holnap (március 9.) nappalos, kedden éjszakás műszakban
                dolgozik, szerdán és csütörtökön pihenőnapos. A brigádból Kiss Nóra március 4. óta influenzás (igazolás március 13-ig),
                Vass Levente pedig március 9–13. között vizsgaidőszak miatt – még januárban jóváhagyott – szabadságon van.

                Most telefonál Németh Krisztián is: belázasodott, az orvosi ügyelet március 11-ig keresőképtelennek írta ki.
                Így hétfő reggel és kedd éjjel az A brigádnak csak 3 gépkezelője lenne a minimális 4 helyett.

                A műszakvezető első ötlete: „Bíró Norbert a C brigádból hétfőn pihenőnapos, hívjuk be reggel 6-ra!”
                Csakhogy a C brigád épp vasárnap este 18:00-tól hétfő reggel 06:00-ig éjszakás műszakban dolgozik –
                Norbert tehát reggel 6-kor jönne le a gépről, és ugyanakkor kellene újrakezdenie.
                """;
    }
    @Override public List<String> getSteps() {
        return Arrays.asList(
                "Töltsd be a helyzetet. A beosztás nézet 2026. márciust mutatja: Kiss Nórán piros [B], Vass Leventén lila [SZ] látható március 9-től.",
                "Az „Éves nyilvántartás” ablakban „➕ Új Távollét”: Németh Krisztián, BETEGSZABADSAG, 2026.03.09 – 2026.03.11, „Lázas megbetegedés”. A rendszer figyelmeztet: március 9. nappalos és március 10. éjszakás műszakjában csak 3 fő marad. „OK”.",
                "Beosztás nézet (március): Németh Krisztián napjai már [B]-vel látszanak. Az „⚡ Automatikus Tervezés” gomb ellenőrzi a tervet, és kilistázza a 2 hiányos műszakot.",
                "Az első ötlet: jobb klikk Bíró Norbert március 9-i celláján → „Nappal (06-18) [N]”, majd „💾 Mentés”.",
                "A mentés megtagadva: „Bíró Norbert: Mt. 104. § súlyos pihenőidő sértés [2026-03-09]: Éjszakai műszak (18-06) után nem osztható be közvetlenül nappalos műszakra…”.",
                "Javítás: Bíró Norbert március 9. → „Pihenőnap [P]”. Helyette Molnár Attila (B brigád, március 8–9. pihenőnapos, 10-én amúgy is nappalos) kapja a március 9-i nappalt.",
                "Március 10. éjszaka: Bíró Norbertet itt sem lehet beosztani, mert a C brigád március 11-én reggel nappalos. Helyette Varga Gergely (D brigád, március 10–11. pihenőnapos) kapja a műszakot.",
                "Mentés: sikeres, nincs szabálysértés és hiányos műszak. Az „Éves nyilvántartás” táblázatában a két helyettes órakontója +11,25 órával nő."
        );
    }
    @Override public String getTakeaway() {
        return """
                • Mt. 104. §: két munkanap között legalább 11 óra egybefüggő pihenőidő jár. Folyamatos, 12 órás
                  műszakrendben ez a leggyakoribb hiba kézi átvezénylésnél (éjszakás után reggeli kezdés = 0 óra pihenő).
                • A program a mentés előtt dolgozónként ellenőrzi a teljes havi tervet, és a szabálysértő beosztást nem engedi
                  publikálni – a hibaüzenet megnevezi a dolgozót, a napot és a jogszabályt.
                • A létszámhiányos műszakok listája megmutatja, pontosan hány helyettes kell és mely napokra.
                • A helyettesítések automatikusan megjelennek az órakontóban, így később látszik, ki vállalt többletmunkát.
                """;
    }

    @Override
    public void define(ScenarioBuilder b) {
        addStandardRoster(b);
        b.sick(KISS, d(3, 4), d(3, 13), "Influenza");
        b.vacation(VASS, d(3, 9), d(3, 13), "Vizsgaidőszak (januárban jóváhagyva)");
    }

    private static LocalDate d(int month, int day) { return LocalDate.of(YEAR, month, day); }
}
