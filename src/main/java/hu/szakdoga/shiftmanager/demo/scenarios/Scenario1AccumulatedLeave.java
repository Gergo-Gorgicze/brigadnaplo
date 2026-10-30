package hu.szakdoga.shiftmanager.demo.scenarios;

import hu.szakdoga.shiftmanager.demo.DemoScenario;
import hu.szakdoga.shiftmanager.demo.ScenarioBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static hu.szakdoga.shiftmanager.demo.DemoRoster.*;

public class Scenario1AccumulatedLeave extends DemoScenario {
    @Override public String getId() { return "1"; }
    @Override public String getTitle() { return "Tamás felhalmozott szabadsága – de a brigád fele beteg"; }
    @Override public String getShortDescription() {
        return "Fekete Tamásnak 31 nap szabadsága maradt, decemberben kivenné, de a C brigádból ketten betegek: a rendszer kimutatja, mely napokon esne 4 fő alá a műszak.";
    }
    @Override public LocalDate getSimulatedToday() { return LocalDate.of(2026, 11, 16); }
    @Override public YearMonth getFocusMonth() { return YearMonth.of(2026, 12); }
    @Override public String getStory() {
        return """
                Fekete Tamás (47 éves, két gyermek) a C brigád legtapasztaltabb gépkezelője. Éves szabadságkerete
                34 munkanap (20 nap alapszabadság + 10 nap életkori + 4 nap gyermekek után járó pótszabadság),
                de idén csak áprilisban vett ki 3 napot: nyáron és ősszel a brigád létszámhiánya miatt rendre bent
                maradt, sőt októberben és novemberben 5 alkalommal a pihenőnapján is bejött helyettesíteni.
                Emiatt november közepén még 31 nap szabadsága van hátra, az órakontója pedig +55,75 óra.

                Közben a C brigádban egyszerre többen kiestek:
                 • Illés Bence október 26. óta térdműtét miatt táppénzen van, az orvos december 18-ig írta ki
                   (a 15 nap betegszabadságot már kimerítette, a többi táppénz),
                 • Kocsis Márton november 12. óta influenzás, majd tüdőgyulladást kapott (igazolás december 9-ig),
                 • Lakatos Zsófia december 21–23. között még nyáron jóváhagyott szabadságon lesz.

                Ma, november 16-án Tamás bejön az irodába, és december 2–11. között (8 munkanap) szeretne
                szabadságra menni. A brigád 6 főből áll, és minden műszakhoz legalább 4 gépkezelő kell.
                Kiadható-e a szabadság? Ha nem, mikor igen? És mi lesz a megmaradó napokkal az év végén?
                """;
    }
    @Override public List<String> getSteps() {
        return Arrays.asList(
                "Töltsd be a helyzetet. A beosztás nézet 2026. decembert mutatja: a C brigád sorában Illés Bence és Kocsis Márton napjain piros [B] (beteg), Lakatos Zsófiánál december 21–23. között lila [SZ] (szabadság) látható.",
                "Kattints a „👥 Éves nyilvántartás” menüre. Fekete Tamás: éves keret 34 nap, kivett 3 nap, hátralévő 31 nap, órakontó +55,75 h. Illés Bence: betegszabadság 15/15 nap, táppénz 25 nap (piros).",
                "Az „Éves nyilvántartás” ablakban kattints az „➕ Új Távollét / Szabadság Rögzítése” gombra. Dolgozó: Fekete Tamás, típus: SZABADSAG, kezdő dátum: 2026.12.02, záró dátum: 2026.12.11, indoklás: „Családi pihenés”. Kattints a „Jóváhagyás & Rögzítés” gombra.",
                "A rendszer „Létszámhiány – Elutasítás javasolt” ablakot nyit: december 4. és 8. nappalos, valamint december 5. és 9. éjszakás műszakjában a C brigádnak csak 3 gépkezelője maradna (minimum 4). Kattints az „Elutasítás” gombra.",
                "Rögzítsd újra a kérelmet 2026.12.14 – 2026.12.23 közötti időszakra (8 munkanap). Ekkor Kocsis Márton már visszatért, így a brigádban a december 16–17-i és 20–21-i műszakokban is marad legalább 4 fő: a rendszer figyelmeztetés nélkül jóváhagyja.",
                "Az „Éves nyilvántartás” táblázatában Tamás kivett szabadsága 11 napra nő, a hátralévő 23 napra csökken. A rögzítés a beosztást is frissíti: a beosztás nézetben (december) Tamás sorában dec. 14–23. között lila [SZ] látható."
        );
    }
    @Override public String getTakeaway() {
        return """
                • Szabadságkeret számítása életkor és gyermekek alapján (Mt. 116–118. §), munkanapokban nyilvántartva.
                • A szabadság kiadásának időpontját a munkáltató határozza meg (Mt. 122. §): a program nem csak a keretet nézi,
                  hanem azt is, hogy a kérelmező kiesése esetén megmarad-e a műszak minimális létszáma.
                  A rendszer napra pontosan megmutatja az ütköző műszakokat, és jogszerű alternatív időpontot lehet keresni.
                • Az Mt. 122. § (2) szerint évi 7 munkanapot a dolgozó kérésére kell kiadni – ezért az elutasítás felülbírálható
                  („Jóváhagyás mégis”), ilyenkor a műszakvezetőnek helyettesítést kell szerveznie.
                • Év végi kockázat: a szabadságot az esedékesség évében kell kiadni (Mt. 123. §). Tamásnak a decemberi
                  szabadság után is 23 napja marad, ami kivételes, a működést súlyosan érintő ok esetén vihető át a következő év elejére.
                  A táblázat ezt időben láthatóvá teszi – Excelben ez csak év végén derülne ki.
                • Betegszabadság és táppénz szétválasztása (Mt. 126. §: évi 15 munkanap), az órakontó vezetése a plusz műszakokkal.
                """;
    }

    @Override
    public void define(ScenarioBuilder b) {
        addStandardRoster(b);
        b.sick(ILLES, d(10, 26), d(12, 18), "Térdműtét és rehabilitáció");
        b.sick(KOCSIS, d(11, 12), d(12, 9), "Influenza, majd tüdőgyulladás (orvosi igazolás dec. 9-ig)");
        b.vacation(LAKATOS, d(12, 21), d(12, 23), "Családi esemény (még nyáron jóváhagyva)");
        for (LocalDate extra : new LocalDate[]{d(10, 12), d(10, 20), d(10, 28), d(11, 5), d(11, 13)}) {
            b.extraShift(FEKETE, extra, "N", "Plusz műszak – helyettesítés");
        }
    }

    private static LocalDate d(int month, int day) { return LocalDate.of(YEAR, month, day); }
}
