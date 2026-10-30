package hu.szakdoga.shiftmanager.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import hu.szakdoga.shiftmanager.model.DailyScheduleRow;
import hu.szakdoga.shiftmanager.model.ShiftType;
import hu.szakdoga.shiftmanager.util.HungarianCalendar;

import java.awt.Color;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A havi beosztás nyomtatható, aláírható PDF dokumentumként (fekvő A4).
 * A munkaidő-beosztást írásban kell közölni a munkavállalókkal (Mt. 97. §); a dokumentum
 * brigádonként csoportosítva tartalmazza a rácsot, dolgozónként a havi nettó órákat
 * és egy aláírási oszlopot, alul a napi létszámot és a műszakvezető aláírási sorát.
 */
public final class SchedulePdfExporter {

    private static final String FONT_REGULAR = "/hu/szakdoga/shiftmanager/fonts/DejaVuSans.ttf";
    private static final String FONT_BOLD = "/hu/szakdoga/shiftmanager/fonts/DejaVuSans-Bold.ttf";
    private static final Locale HU = new Locale("hu", "HU");
    private static final String[] WEEKDAYS = {"H", "K", "Sze", "Cs", "P", "Szo", "V"};

    private static final Color HEADER_BG = new Color(0x2D, 0x37, 0x48);
    private static final Color WEEKEND_BG = new Color(0xE2, 0xE8, 0xF0);
    private static final Color TEAM_BG = new Color(0xED, 0xF2, 0xF7);

    private SchedulePdfExporter() {}

    /** A cellák háttérszíne műszakkódonként (a felület színeinek világos, nyomtatóbarát változata). */
    static Color colorFor(String code) {
        switch (code) {
            case "N":  return new Color(0xFF, 0xE0, 0xB2);
            case "É":  return new Color(0xBB, 0xDE, 0xFB);
            case "P":  return Color.WHITE;
            case "SZ": return new Color(0xE1, 0xBE, 0xE7);
            case "B":  return new Color(0xFF, 0xCD, 0xD2);
            case "F":  return new Color(0xE0, 0xE0, 0xE0);
            default:   return Color.WHITE;
        }
    }

    /**
     * A hónap beosztásának kiírása PDF formátumban.
     *
     * @param month      a hónap
     * @param rows       a rács sorai (dolgozónként a napi műszakokkal)
     * @param preparedOn a dokumentumon feltüntetett készítési dátum
     * @param out        a kimeneti adatfolyam (a hívó zárja le)
     */
    public static void export(YearMonth month, List<DailyScheduleRow> rows, LocalDate preparedOn,
                              OutputStream out) throws Exception {
        BaseFont regular = loadFont(FONT_REGULAR);
        BaseFont bold = loadFont(FONT_BOLD);
        Font titleFont = new Font(bold, 14);
        Font textFont = new Font(regular, 8);
        Font cellFont = new Font(regular, 6.5f);
        Font cellBold = new Font(bold, 6.5f);
        Font headerFont = new Font(bold, 6.5f, Font.NORMAL, Color.WHITE);

        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 24, 24);
        PdfWriter.getInstance(doc, out);
        doc.addTitle("Havi munkaidő-beosztás – " + monthLabel(month));
        doc.addCreator("Brigádnapló");
        doc.open();

        doc.add(new Paragraph("Havi munkaidő-beosztás – " + monthLabel(month), titleFont));
        doc.add(new Paragraph("4 műszakos folytonos munkarend (N–É–P–P rotáció)   ·   Készült: "
                + preparedOn.format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")), textFont));

        int days = month.lengthOfMonth();
        PdfPTable table = new PdfPTable(days + 3);
        float[] widths = new float[days + 3];
        widths[0] = 70;
        for (int d = 1; d <= days; d++) widths[d] = 15.5f;
        widths[days + 1] = 24;
        widths[days + 2] = 70;
        table.setWidths(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setHeaderRows(1);

        // Fejléc: nap sorszáma és a hét napja; hétvége és munkaszüneti nap kiemelve
        table.addCell(cell("Dolgozó", headerFont, HEADER_BG, Element.ALIGN_LEFT));
        for (int d = 1; d <= days; d++) {
            LocalDate date = month.atDay(d);
            String label = d + "\n" + WEEKDAYS[date.getDayOfWeek().getValue() - 1];
            Color bg = isOffDay(date) ? new Color(0x4A, 0x55, 0x68) : HEADER_BG;
            table.addCell(cell(label, headerFont, bg, Element.ALIGN_CENTER));
        }
        table.addCell(cell("Óra", headerFont, HEADER_BG, Element.ALIGN_CENTER));
        table.addCell(cell("Aláírás", headerFont, HEADER_BG, Element.ALIGN_CENTER));

        // Dolgozók brigádonként csoportosítva
        Map<String, List<DailyScheduleRow>> byTeam = new LinkedHashMap<>();
        for (DailyScheduleRow row : rows) {
            byTeam.computeIfAbsent(row.getTeamName(), k -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<String, List<DailyScheduleRow>> team : byTeam.entrySet()) {
            PdfPCell teamCell = cell(team.getKey(), cellBold, TEAM_BG, Element.ALIGN_LEFT);
            teamCell.setColspan(days + 3);
            table.addCell(teamCell);
            for (DailyScheduleRow row : team.getValue()) {
                table.addCell(cell(row.getEmployeeName(), cellFont, Color.WHITE, Element.ALIGN_LEFT));
                for (int d = 1; d <= days; d++) {
                    ShiftType shift = row.getShift(d);
                    String code = shift != null ? shift.getCode() : "";
                    table.addCell(cell(code, cellFont, colorFor(code), Element.ALIGN_CENTER));
                }
                table.addCell(cell(formatHours(row.calculateTotalHours()), cellFont, Color.WHITE, Element.ALIGN_RIGHT));
                table.addCell(cell("", cellFont, Color.WHITE, Element.ALIGN_LEFT));
            }
        }

        // Napi létszám műszakonként
        addCountRow(table, rows, days, "N", "Nappal (fő)", cellBold, cellFont);
        addCountRow(table, rows, days, "É", "Éjszaka (fő)", cellBold, cellFont);
        doc.add(table);

        doc.add(new Paragraph("Jelmagyarázat: N = nappal (06–18), É = éjszaka (18–06), P = pihenőnap, "
                + "SZ = szabadság, B = betegszabadság / táppénz, F = fizetés nélküli szabadság. "
                + "Az órák a munkaközi szünet nélküli nettó munkaidőt mutatják (Mt. 103. §).", textFont));
        doc.add(new Paragraph("A munkaidő-beosztást legalább hét nappal korábban, írásban kell közölni (Mt. 97. §). "
                + "Aláírásommal igazolom, hogy a beosztást megismertem.", textFont));
        Paragraph signature = new Paragraph("\nKészítette (műszakvezető): ..........................................."
                + "          Dátum: ..............................", textFont);
        doc.add(signature);
        doc.close();
    }

    /** Hány dolgozó dolgozik a megadott műszakban az adott napon. */
    static int countOnDay(List<DailyScheduleRow> rows, int day, String code) {
        int count = 0;
        for (DailyScheduleRow row : rows) {
            ShiftType shift = row.getShift(day);
            if (shift != null && code.equals(shift.getCode())) count++;
        }
        return count;
    }

    static String monthLabel(YearMonth month) {
        String name = month.getMonth().getDisplayName(TextStyle.FULL, HU);
        return month.getYear() + ". " + name;
    }

    private static void addCountRow(PdfPTable table, List<DailyScheduleRow> rows, int days, String code,
                                    String label, Font labelFont, Font font) {
        table.addCell(cell(label, labelFont, TEAM_BG, Element.ALIGN_LEFT));
        for (int d = 1; d <= days; d++) {
            table.addCell(cell(String.valueOf(countOnDay(rows, d, code)), font, TEAM_BG, Element.ALIGN_CENTER));
        }
        PdfPCell rest = cell("", font, TEAM_BG, Element.ALIGN_LEFT);
        rest.setColspan(2);
        table.addCell(rest);
    }

    private static boolean isOffDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY || HungarianCalendar.isHoliday(date);
    }

    private static String formatHours(double hours) {
        return hours == Math.rint(hours) ? String.valueOf((long) hours) : String.format(HU, "%.2f", hours);
    }

    private static PdfPCell cell(String text, Font font, Color background, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(background);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(2);
        c.setBorderWidth(0.4f);
        c.setBorder(Rectangle.BOX);
        return c;
    }

    /** A magyar ékezetes betűkhöz (ő, ű) beágyazott Unicode betűkészlet kell. */
    private static BaseFont loadFont(String resource) throws Exception {
        try (InputStream in = SchedulePdfExporter.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("A betűkészlet nem található: " + resource);
            }
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(resource.substring(resource.lastIndexOf('/') + 1), BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, true, bytes, null);
        }
    }
}
