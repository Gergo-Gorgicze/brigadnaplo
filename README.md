# Brigádnapló – Négy műszakos óranyilvántartó és vezénylési rendszer

Szakdolgozati projekt (Debreceni Egyetem, Informatikai Kar): asztali alkalmazás a 4 műszakos, 12 órás
folyamatos munkarend tervezésére és a Munka Törvénykönyve (2012. évi I. tv.) szerinti munkaidő- és
szabadság-nyilvántartásra. Egyfelhasználós eszköz a műszakvezető számára.

## Funkciók

- **N–É–P–P vezénylés:** a 4 brigád (A, B, C, D) havi beosztásának automatikus generálása
  (`D, É, P, P` ciklus), minden napra 1 nappalos és 1 éjszakás brigáddal.
- **Munkajogi ellenőrzés mentéskor:** éjszakás műszak után másnap nappalos műszak (0 óra pihenő, Mt. 104. §)
  és 12 óránál hosszabb napi munkaidő (Mt. 99. §) esetén a mentés megtagadva.
- **Minimális műszaklétszám:** a hiányos műszakok listázása; szabadság rögzítésekor elutasítási javaslat,
  betegség rögzítésekor figyelmeztetés.
- **Éves nyilvántartás:** szabadságkeret (Mt. 115–117., 120. §), kivett és hátralévő napok, betegszabadság
  és táppénz (Mt. 126. §), fizetés nélküli szabadság, órakontó (munkaidőkeret-egyenleg).
- **Távollétek rögzítése:** szabadság, betegség, fizetés nélküli szabadság; a beosztás automatikusan frissül.
- **Havi pótlékos órák:** éjszakai (30% műszakpótlék, Mt. 141. §), vasárnapi és munkaszüneti napi (Mt. 140. §) órák.
- **PDF export:** a mentett havi beosztás fekvő A4-es, aláírható PDF dokumentumként (Mt. 97. §: írásbeli közlés).
- **Demó helyzetek:** 6 előre elkészített állapot (alapállapot + 5 életszerű eset) egy 24 fős üzem 2026-os
  évével, szimulált „mai nappal”.

## Technológia

Java 17, JavaFX 21, SQLite (sqlite-jdbc) + HikariCP, JUnit 5, Maven.
Az adatbázis a munkakönyvtárban jön létre (`shiftmanager.db`); első indításkor a program betölti az alapállapotot.

## Indítás IntelliJ IDEA-ban

1. **File → Open**, a `ShiftManager` mappa kiválasztása (a Maven projekt automatikusan betöltődik).
2. `src/main/java/hu/szakdoga/shiftmanager/AppLauncher.java` → jobb klikk → **Run 'AppLauncher.main()'**.

Parancssorból: `mvnw javafx:run`

## Tesztek

`mvnw test` vagy IntelliJ-ben jobb klikk az `src/test/java` mappán → **Run 'All Tests'**.
24 teszt: rotáció, munkajogi szabályok, szabadságkeret, demó adatok, valamint integrációs tesztek ideiglenes
SQLite adatbázissal (köztük egy 100 fős teljesítménymérés, amely kiírja a mért időket).

## Bemutatás

A bal oldali menü **🎬 Demó helyzetek** pontjában választható ki egy helyzet; a fejléc **ℹ Leírás** gombja
megmutatja a történetet és a bemutatás lépéseit. A **0. Alapállapot** bármikor visszatölthető.
