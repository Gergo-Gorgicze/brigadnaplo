package hu.szakdoga.shiftmanager.demo;

import java.time.LocalDate;

/**
 * A demó helyzetek közös üzemi létszáma: egy kitalált, folyamatos üzemű műanyag-feldolgozó
 * üzem 4 brigádja, brigádonként 6 gépkezelővel (24 fő). Minden műszakhoz legalább 4 fő kell.
 *
 * A háttéradatok az év "szokásos" távolléteit tartalmazzák: tavaszi rövid szabadság,
 * kéthetes nyári szabadság (brigádon belül egymás után ütemezve, hogy a létszám ne essen
 * 4 fő alá), őszi szabadságok és néhány rövid megbetegedés. Ezekre épülnek rá az egyes
 * helyzetek saját eseményei.
 */
public final class DemoRoster {

    public static final int YEAR = 2026;

    // A brigád (team 1)
    public static final String NEMETH = "nemeth", KISS = "kiss", SZILAGYI = "szilagyi",
            PINTER = "pinter", FARKAS = "farkas", VASS = "vass";
    // B brigád (team 2)
    public static final String BALOGH = "balogh", PAPP = "papp", SZUCS = "szucs",
            MOLNAR = "molnar", FODOR = "fodor", SIMON = "simon";
    // C brigád (team 3)
    public static final String FEKETE = "fekete", ILLES = "illes", KOCSIS = "kocsis",
            LAKATOS = "lakatos", BIRO = "biro", MAJOR = "major";
    // D brigád (team 4)
    public static final String HORVATH = "horvath", TAKACS = "takacs", VARGA = "varga",
            OLAH = "olah", KOZMA = "kozma", RACZ = "racz";

    private static final String[][] BRIGADES = {
            {NEMETH, KISS, SZILAGYI, PINTER, FARKAS, VASS},
            {BALOGH, PAPP, SZUCS, MOLNAR, FODOR, SIMON},
            {FEKETE, ILLES, KOCSIS, LAKATOS, BIRO, MAJOR},
            {HORVATH, TAKACS, VARGA, OLAH, KOZMA, RACZ}
    };

    private DemoRoster() {}

    static LocalDate d(int month, int day) {
        return LocalDate.of(YEAR, month, day);
    }

    /** A 24 fős állomány és az év szokásos (háttér) távollétei. */
    public static void addStandardRoster(ScenarioBuilder b) {
        // --- A brigád ---
        b.person(NEMETH, "Németh", "Krisztián", 1, LocalDate.of(1986, 4, 3), LocalDate.of(2012, 3, 1), 2, false);
        b.person(KISS, "Kiss", "Nóra", 1, LocalDate.of(1994, 9, 17), LocalDate.of(2019, 9, 1), 1, false);
        b.person(SZILAGYI, "Szilágyi", "Eszter", 1, LocalDate.of(1990, 1, 25), LocalDate.of(2016, 5, 2), 2, false);
        b.person(PINTER, "Pintér", "Gábor", 1, LocalDate.of(1975, 11, 8), LocalDate.of(2008, 8, 1), 3, false);
        b.person(FARKAS, "Farkas", "Dóra", 1, LocalDate.of(1992, 6, 30), LocalDate.of(2018, 1, 15), 2, false);
        b.person(VASS, "Vass", "Levente", 1, LocalDate.of(2001, 2, 14), LocalDate.of(2022, 6, 1), 0, false);
        // --- B brigád ---
        b.person(BALOGH, "Balogh", "Ádám", 2, LocalDate.of(1983, 3, 22), LocalDate.of(2010, 4, 1), 1, false);
        b.person(PAPP, "Papp", "Rita", 2, LocalDate.of(1997, 7, 9), LocalDate.of(2021, 2, 1), 0, false);
        b.person(SZUCS, "Szűcs", "Dániel", 2, LocalDate.of(1989, 12, 1), LocalDate.of(2015, 10, 1), 2, false);
        b.person(MOLNAR, "Molnár", "Attila", 2, LocalDate.of(1980, 5, 16), LocalDate.of(2009, 1, 5), 1, false);
        b.person(FODOR, "Fodor", "Bianka", 2, LocalDate.of(1999, 10, 27), LocalDate.of(2023, 3, 1), 0, false);
        b.person(SIMON, "Simon", "Csaba", 2, LocalDate.of(1971, 8, 4), LocalDate.of(2005, 9, 1), 2, false);
        // --- C brigád ---
        b.person(FEKETE, "Fekete", "Tamás", 3, LocalDate.of(1979, 5, 12), LocalDate.of(2007, 2, 1), 2, false);
        b.person(ILLES, "Illés", "Bence", 3, LocalDate.of(1985, 2, 19), LocalDate.of(2014, 6, 1), 1, false);
        b.person(KOCSIS, "Kocsis", "Márton", 3, LocalDate.of(1993, 11, 11), LocalDate.of(2020, 11, 2), 0, false);
        b.person(LAKATOS, "Lakatos", "Zsófia", 3, LocalDate.of(1988, 3, 6), LocalDate.of(2013, 4, 15), 2, false);
        b.person(BIRO, "Bíró", "Norbert", 3, LocalDate.of(1996, 9, 2), LocalDate.of(2021, 8, 16), 0, false);
        b.person(MAJOR, "Major", "Erika", 3, LocalDate.of(1977, 12, 15), LocalDate.of(2006, 3, 1), 3, true);
        // --- D brigád ---
        b.person(HORVATH, "Horváth", "Richárd", 4, LocalDate.of(1991, 7, 21), LocalDate.of(2017, 7, 1), 0, false);
        b.person(TAKACS, "Takács", "Péter", 4, LocalDate.of(1984, 1, 30), LocalDate.of(2011, 5, 2), 2, false);
        b.person(VARGA, "Varga", "Gergely", 4, LocalDate.of(1987, 5, 5), LocalDate.of(2015, 2, 1), 1, false);
        b.person(OLAH, "Oláh", "Viktória", 4, LocalDate.of(1995, 4, 18), LocalDate.of(2020, 1, 6), 1, false);
        b.person(KOZMA, "Kozma", "István", 4, LocalDate.of(1969, 10, 10), LocalDate.of(2003, 9, 1), 2, false);
        b.person(RACZ, "Rácz", "Dávid", 4, LocalDate.of(2000, 6, 12), LocalDate.of(2023, 9, 1), 0, false);

        addBackgroundLeaves(b);
    }

    private static void addBackgroundLeaves(ScenarioBuilder b) {
        // Tavaszi rövid szabadság (szerda–péntek), brigádon belül hetente más-más dolgozó
        LocalDate[][] spring = {
                {d(4, 8), d(4, 10)}, {d(4, 15), d(4, 17)}, {d(4, 22), d(4, 24)},
                {d(4, 29), d(5, 1)}, {d(5, 6), d(5, 8)}, {d(5, 13), d(5, 15)}
        };
        // Kéthetes nyári szabadság (hétfőtől a következő hét péntekéig), egymás után ütemezve
        LocalDate[][] summer = {
                {d(6, 8), d(6, 19)}, {d(6, 22), d(7, 3)}, {d(7, 6), d(7, 17)},
                {d(7, 20), d(7, 31)}, {d(8, 3), d(8, 14)}, {d(8, 17), d(8, 28)}
        };
        // Őszi egyhetes szabadság (minden második dolgozónak)
        LocalDate[][] autumn = {
                {d(9, 14), d(9, 18)}, null, {d(9, 28), d(10, 2)}, null, {d(10, 12), d(10, 16)}, null
        };

        for (String[] brigade : BRIGADES) {
            for (int i = 0; i < brigade.length; i++) {
                String key = brigade[i];
                b.vacation(key, spring[i][0], spring[i][1], "Tavaszi szabadság");
                if (FEKETE.equals(key)) {
                    // Fekete Tamás idén nyáron és ősszel sem ment szabadságra: a brigád létszámhiánya
                    // miatt többször helyettesített, ezért a szabadsága felhalmozódott (1. helyzet).
                    continue;
                }
                b.vacation(key, summer[i][0], summer[i][1], "Nyári szabadság (éves szabadságolási terv)");
                if (autumn[i] != null) {
                    b.vacation(key, autumn[i][0], autumn[i][1], "Őszi szabadság");
                }
            }
        }

        // Rövid megbetegedések az év első felében / ősszel
        b.sick(VASS, d(1, 19), d(1, 23), "Megfázás");
        b.sick(FODOR, d(4, 20), d(4, 24), "Influenza");
        b.sick(BIRO, d(5, 11), d(5, 13), "Gyomorrontás");
        b.sick(OLAH, d(9, 7), d(9, 11), "Felső légúti fertőzés");
    }
}
