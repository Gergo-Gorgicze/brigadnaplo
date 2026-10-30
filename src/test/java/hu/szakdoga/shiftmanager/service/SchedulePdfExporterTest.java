package hu.szakdoga.shiftmanager.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import hu.szakdoga.shiftmanager.demo.DemoRoster;
import hu.szakdoga.shiftmanager.demo.DemoScenario;
import hu.szakdoga.shiftmanager.demo.ScenarioBuilder;
import hu.szakdoga.shiftmanager.demo.ScenarioCatalog;
import hu.szakdoga.shiftmanager.model.DailyScheduleRow;
import hu.szakdoga.shiftmanager.model.User;
import hu.szakdoga.shiftmanager.model.WorkSchedule;
import hu.szakdoga.shiftmanager.util.AppClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A havi beosztás PDF exportjának ellenőrzése az alapállapot 2026. júniusi adatain, adatbázis nélkül.
 */
public class SchedulePdfExporterTest {

    @AfterEach
    public void resetClock() {
        AppClock.reset();
    }

    private static List<DailyScheduleRow> juneRows() {
        DemoScenario base = ScenarioCatalog.byId("0");
        ScenarioBuilder b = new ScenarioBuilder();
        base.define(b);
        ScenarioBuilder.Materialized m = b.materialize(base.getSimulatedToday(), DemoRoster.YEAR,
                ScenarioCatalog.standardShiftTypes());
        Map<Integer, DailyScheduleRow> rows = new LinkedHashMap<>();
        for (User u : m.users) rows.put(u.getId(), new DailyScheduleRow(u));
        for (WorkSchedule ws : m.schedules) {
            if (ws.getDate().getYear() == 2026 && ws.getDate().getMonthValue() == 6) {
                rows.get(ws.getUser().getId()).setShift(ws.getDate().getDayOfMonth(), ws.getShiftType());
            }
        }
        return new ArrayList<>(rows.values());
    }

    @Test
    @DisplayName("PDF export: érvényes, egyoldalas dokumentum magyar ékezetekkel, címmel és aláírási sorral")
    public void testExportProducesReadablePdf() throws Exception {
        List<DailyScheduleRow> rows = juneRows();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SchedulePdfExporter.export(YearMonth.of(2026, 6), rows, LocalDate.of(2026, 6, 1), out);

        byte[] pdf = out.toByteArray();
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4));

        PdfReader reader = new PdfReader(pdf);
        assertEquals(1, reader.getNumberOfPages(), "24 dolgozó havi beosztása egy fekvő A4 oldalra fér");
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();

        assertTrue(text.contains("Havi munkaidő-beosztás – 2026. június"), "cím");
        assertTrue(text.contains("Szűcs Dániel"), "ő/ű betűk a beágyazott betűkészlettel");
        assertTrue(text.contains("A Brigád") && text.contains("D Brigád"), "brigádonkénti csoportosítás");
        assertTrue(text.contains("Készítette (műszakvezető)"), "aláírási sor");
    }

    @Test
    @DisplayName("PDF export: a napi létszámsorok a rotáció szerinti létszámot mutatják")
    public void testDailyCountsMatchRotation() {
        List<DailyScheduleRow> rows = juneRows();
        for (int day = 1; day <= 30; day++) {
            int d = SchedulePdfExporter.countOnDay(rows, day, "N");
            int e = SchedulePdfExporter.countOnDay(rows, day, "É");
            assertTrue(d >= 4 && d <= 6, "nappal " + day + ". nap: " + d);
            assertTrue(e >= 4 && e <= 6, "éjszaka " + day + ". nap: " + e);
        }
    }
}
