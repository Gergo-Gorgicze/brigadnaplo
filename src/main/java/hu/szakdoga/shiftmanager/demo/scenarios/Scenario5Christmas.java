package hu.szakdoga.shiftmanager.demo.scenarios;

import hu.szakdoga.shiftmanager.demo.DemoScenario;
import hu.szakdoga.shiftmanager.demo.ScenarioBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static hu.szakdoga.shiftmanager.demo.DemoRoster.*;

public class Scenario5Christmas extends DemoScenario {
    @Override public String getId() { return "5"; }
    @Override public String getTitle() { return "Karácsonyi szabadságok – kinek jut szabadnap az ünnepek alatt?"; }
    @Override public String getShortDescription() {
        return "Az A brigádból ketten már októberben megkapták a karácsonyi szabadságot. Farkas Dóra a szenteste előtti napokra is szabadságot kérne, de a brigád létszáma ezt nem bírja el – a megoldás egy műszakcsere.";
    }
    @Override public LocalDate getSimulatedToday() { return LocalDate.of(2026, 12, 1); }
    @Override public YearMonth getFocusMonth() { return YearMonth.of(2026, 12); }
    @Override public String getStory() {
        return """
                December 1. Az A brigád beosztása az ünnepek körül: december 22. nappal, 23. éjszaka (reggel 6-kor
                ér véget, szenteste napján), 24–25. pihenőnap, 26. nappal, 27. éjszaka, 30. nappal és 31. éjszaka (szilveszter).

                Szilágyi Eszter (december 21. – január 1.) és Pintér Gábor (december 22–31.) még októberben benyújtották
                és megkapták a karácsonyi szabadságukat – a brigád így ezekben a napokban pontosan 4 fővel, a minimummal dolgozik.

                Farkas Dóra (két kisgyermekes anya) ma kéri, hogy december 22–23-án szabadságon lehessen, hogy
                szentestére kipihenten hazautazhasson a szüleihez. Szabadságkerete van bőven (28 napból 10 nap még kivehető),
                a „kérésre kiadandó” évi 7 napját viszont nyáron már felhasználta. A kérdés: ki lehet-e adni a szabadságot,
                és ha nem, hogyan lehet mégis segíteni rajta úgy, hogy a karácsonyi műszakok is szabályosak maradjanak?
                """;
    }
    @Override public List<String> getSteps() {
        return Arrays.asList(
                "Töltsd be a helyzetet. A beosztás nézet 2026. decembert mutatja: az A brigádban Szilágyi Eszter és Pintér Gábor napjain lila [SZ] látható.",
                "Az „Éves nyilvántartás” ablakban „➕ Új Távollét”: Farkas Dóra, SZABADSAG, 2026.12.22 – 2026.12.23, „Szenteste a családdal”. A rendszer létszámhiányt jelez december 22. (nappal) és 23. (éjszaka) műszakjában: csak 3 fő maradna. „Elutasítás”.",
                "Próbáld meg december 28–31. között: december 28–29. az A brigád amúgy is pihenőnapja, december 30-án (nappal) és 31-én (szilveszter éjjel) viszont szintén hiány lenne – ez is elutasítandó.",
                "Kompromisszum – műszakcsere: a beosztás nézetben jobb klikk Farkas Dóra december 23-i celláján → „Pihenőnap [P]”. Az első ötlet Major Erika (C brigád, 23-án pihenőnapos) lenne: az ő december 23-i cellája → „Éjszaka (18-06) [É]”, majd „💾 Mentés”.",
                "A mentés megtagadva: a C brigád december 24-én reggel nappalos, Major Erika tehát 0 óra pihenővel kezdene (Mt. 104. §). Állítsd vissza a cellát „Pihenőnap [P]”-re, és helyette Molnár Attila (B brigád, december 23–24. pihenőnapos) kapja a december 23-i éjszakát.",
                "„💾 Mentés”: a terv szabályos, és az A brigád december 23-i éjszakás műszakja ismét 4 fős.",
                "„📊 Havi pótlékok” (december): a december 25–26-i munkaszüneti napokon ledolgozott órák külön sorban jelennek meg (100% pótlék), az éjszakai és vasárnapi órák mellett.",
                "„👥 Éves nyilvántartás”: Farkas Dóra órakontója 11,25 órával csökken, Molnár Attiláé ugyanennyivel nő – a csere az éves elszámolásban is nyomon követhető."
        );
    }
    @Override public String getTakeaway() {
        return """
                • Az előre jóváhagyott (tervezett) szabadságok élveznek elsőbbséget; a később érkező kérelmeknél a program
                  azonnal megmutatja, hogy a brigád létszáma melyik ünnepi műszakban esne a minimum alá.
                • Az elutasítás nem „a gép döntése”: a műszakvezető látja az okot, és egy célzott műszakcserével
                  (a 11 órás pihenőidő automatikus ellenőrzése mellett) emberséges megoldást találhat.
                • Munkaszüneti napi (Mt. 140. §, 100%), éjszakai (30% műszakpótlék) és vasárnapi órák havi kimutatása.
                • Minden változás (csere, szabadság) azonnal megjelenik az éves órakontóban és szabadság-nyilvántartásban.
                """;
    }

    @Override
    public void define(ScenarioBuilder b) {
        addStandardRoster(b);
        b.vacation(SZILAGYI, d(12, 21), LocalDate.of(YEAR + 1, 1, 1), "Karácsonyi szabadság (októberben jóváhagyva)");
        b.vacation(PINTER, d(12, 22), d(12, 31), "Karácsonyi szabadság (októberben jóváhagyva)");
    }

    private static LocalDate d(int month, int day) { return LocalDate.of(YEAR, month, day); }
}
