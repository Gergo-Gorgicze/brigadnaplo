package hu.szakdoga.shiftmanager.controller;

import hu.szakdoga.shiftmanager.dao.AppSettingsDAO;
import hu.szakdoga.shiftmanager.dao.ScheduleDAO;
import hu.szakdoga.shiftmanager.dao.TimeOffDAO;
import hu.szakdoga.shiftmanager.dao.ShiftTypeDAO;
import hu.szakdoga.shiftmanager.dao.UserDAO;
import hu.szakdoga.shiftmanager.demo.DemoScenario;
import hu.szakdoga.shiftmanager.demo.ScenarioLoader;
import hu.szakdoga.shiftmanager.demo.ScenarioPickerDialog;
import hu.szakdoga.shiftmanager.model.AnnualLeaveBalance;
import hu.szakdoga.shiftmanager.model.DailyScheduleRow;
import hu.szakdoga.shiftmanager.model.ShiftType;
import hu.szakdoga.shiftmanager.model.User;
import hu.szakdoga.shiftmanager.model.WorkSchedule;
import hu.szakdoga.shiftmanager.service.AnnualTimeTrackingService;
import hu.szakdoga.shiftmanager.service.AutoSchedulerService;
import hu.szakdoga.shiftmanager.service.MtLaborRuleValidator;
import hu.szakdoga.shiftmanager.service.SchedulePdfExporter;
import hu.szakdoga.shiftmanager.service.StaffingService;
import hu.szakdoga.shiftmanager.service.SurchargeCalculator;
import hu.szakdoga.shiftmanager.util.AppClock;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class ScheduleGridController implements Initializable {

    @FXML private Label monthTitleLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<DailyScheduleRow> scheduleTable;
    @FXML private Label viewSubtitleLabel;
    @FXML private HBox scenarioBannerBox;
    @FXML private Label scenarioBannerLabel;

    private final UserDAO userDAO = new UserDAO();
    private final ShiftTypeDAO shiftTypeDAO = new ShiftTypeDAO();
    private final ScheduleDAO scheduleDAO = new ScheduleDAO();
    private final AppSettingsDAO settingsDAO = new AppSettingsDAO();
    private final AutoSchedulerService autoSchedulerService = new AutoSchedulerService();
    private final MtLaborRuleValidator laborRuleValidator = new MtLaborRuleValidator();
    private final TimeOffDAO timeOffDAO = new TimeOffDAO();
    private final StaffingService staffingService = new StaffingService();
    private final ScenarioLoader scenarioLoader = new ScenarioLoader();

    private YearMonth currentYearMonth = YearMonth.now();
    private DemoScenario activeScenario;
    private final ObservableList<DailyScheduleRow> tableRows = FXCollections.observableArrayList();
    private Map<String, ShiftType> shiftTypeMap;
    /** Van-e a rácson még nem mentett (kézi vagy generált) módosítás. */
    private boolean hasUnsavedChanges = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        shiftTypeMap = shiftTypeDAO.getShiftTypesByCodeMap();
        scheduleTable.setItems(tableRows);

        // A korábban betöltött helyzet folytatása; üres adatbázisnál az alapállapot betöltése
        try {
            activeScenario = scenarioLoader.restoreOrLoadDefault();
            currentYearMonth = scenarioLoader.getFocusMonth(activeScenario);
        } catch (Exception e) {
            e.printStackTrace();
            activeScenario = null;
            currentYearMonth = AppClock.currentMonth();
            new Alert(Alert.AlertType.ERROR, "Az adatok betöltése nem sikerült: " + e.getMessage()).showAndWait();
        }
        updateScenarioBanner();
        loadMonthData(currentYearMonth);
    }

    /** A fejléc demó sávjának frissítése (aktív helyzet + szimulált mai nap). */
    private void updateScenarioBanner() {
        if (scenarioBannerBox == null) return;
        boolean active = activeScenario != null;
        scenarioBannerBox.setVisible(active);
        scenarioBannerBox.setManaged(active);
        if (active) {
            scenarioBannerLabel.setText("🎬 Helyzet: " + activeScenario.getDisplayName() + "   ·   Szimulált mai nap: "
                    + AppClock.today().format(DateTimeFormatter.ofPattern("yyyy.MM.dd."))
                    + (scenarioLoader.isActiveScenarioModified() ? "   ·   ✎ módosítva" : ""));
        }
    }

    /** Hosszabb szöveges riport megjelenítése görgethető mezőben. */
    private Optional<ButtonType> showReport(Alert.AlertType type, String title, String header, String body, ButtonType... buttons) {
        return showReport(scheduleTable.getScene() != null ? scheduleTable.getScene().getWindow() : null,
                type, title, header, body, buttons);
    }

    private Optional<ButtonType> showReport(javafx.stage.Window owner, Alert.AlertType type, String title, String header,
                                            String body, ButtonType... buttons) {
        Alert alert = buttons.length > 0 ? new Alert(type, "", buttons) : new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        TextArea area = new TextArea(body);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(720, Math.min(460, 90 + 18 * body.split("\n").length));
        alert.getDialogPane().setContent(area);
        alert.setResizable(true);
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert.showAndWait();
    }

    /**
     * Hónap adatainak betöltése és a táblázat oszlopainak dinamikus újraépítése.
     */
    public void loadMonthData(YearMonth ym) {
        this.currentYearMonth = ym;
        updateMonthHeader();
        buildTableColumns();
        loadSchedulesFromDatabase();
    }

    private void updateMonthHeader() {
        String monthName = currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("hu", "HU"));
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
        monthTitleLabel.setText(currentYearMonth.getYear() + ". " + monthName);
    }

    private void buildTableColumns() {
        scheduleTable.getColumns().clear();

        // 1. Oszlop: Dolgozó neve
        TableColumn<DailyScheduleRow, String> nameCol = new TableColumn<>("Név / Dolgozó");
        nameCol.setCellValueFactory(data -> data.getValue().employeeNameProperty());
        nameCol.setPrefWidth(180);
        nameCol.setSortable(false);
        scheduleTable.getColumns().add(nameCol);

        // 2. Oszlop: Brigád
        TableColumn<DailyScheduleRow, String> teamCol = new TableColumn<>("Brigád");
        teamCol.setCellValueFactory(data -> data.getValue().teamNameProperty());
        teamCol.setPrefWidth(100);
        teamCol.setSortable(false);
        scheduleTable.getColumns().add(teamCol);

        // 3. Dinamikus napi oszlopok (01, 02, ..., 31)
        int daysInMonth = currentYearMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            final int dayNum = day;
            LocalDate date = currentYearMonth.atDay(dayNum);
            String dayOfWeekShort = getHungarianDayOfWeekShort(date.getDayOfWeek());

            // Az (esetleg szimulált) mai naphoz viszonyítva: eltelt nap, mai nap vagy még tervezett nap.
            // A TableColumn stílusosztálya a fejlécre és az oszlop minden cellájára is érvényes.
            LocalDate today = AppClock.today();
            boolean isToday = date.equals(today);
            boolean isPast = date.isBefore(today);

            TableColumn<DailyScheduleRow, ShiftType> dayCol = new TableColumn<>(
                    String.format("%02d\n%s\n%s", dayNum, dayOfWeekShort, isToday ? "MA" : "")
            );
            dayCol.setPrefWidth(46);
            dayCol.setSortable(false);
            if (isToday) {
                dayCol.getStyleClass().add("day-today");
            } else if (isPast) {
                dayCol.getStyleClass().add("day-past");
            }

            dayCol.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getShift(dayNum))
            );

            dayCol.setCellFactory(column -> new TableCell<DailyScheduleRow, ShiftType>() {
                @Override
                protected void updateItem(ShiftType shift, boolean empty) {
                    super.updateItem(shift, empty);
                    if (empty || shift == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        Label badge = new Label(shift.getCode());
                        badge.getStyleClass().add("badge-label");

                        switch (shift.getCode()) {
                            case "N":
                                badge.getStyleClass().add("badge-day");
                                break;
                            case "É":
                                badge.getStyleClass().add("badge-night");
                                break;
                            case "P":
                                badge.getStyleClass().add("badge-rest");
                                break;
                            case "SZ":
                                badge.getStyleClass().add("badge-leave");
                                break;
                            case "B":
                                badge.getStyleClass().add("badge-sick");
                                break;
                            case "F":
                                badge.getStyleClass().add("badge-unpaid");
                                break;
                        }

                        badge.setTooltip(new Tooltip(shift.getName()
                                + (isToday ? " (mai nap)" : isPast ? " (eltelt nap)" : "")));
                        setAlignment(Pos.CENTER);
                        setGraphic(badge);

                        // Jobb klikkes gyors műszakváltó menü; a távolléti napok (SZ, B, F) itt nem módosíthatók
                        boolean absence = !shift.isWorkingShift() && !"P".equals(shift.getCode());
                        setContextMenu(absence ? null : createShiftContextMenu(getTableRow().getItem(), dayNum));
                    }
                }
            });

            scheduleTable.getColumns().add(dayCol);
        }
    }

    private ContextMenu createShiftContextMenu(DailyScheduleRow row, int dayNum) {
        if (row == null) return null;
        ContextMenu menu = new ContextMenu();
        // Kézzel csak munkavégzés vagy pihenőnap állítható be; a távolléteket az éves nyilvántartásban kell rögzíteni,
        // hogy a szabadság- és betegszabadság-egyenleg mindig a rögzített távollétekből számolódjon.
        for (String code : new String[]{"N", "É", "P"}) {
            ShiftType st = shiftTypeMap.get(code);
            if (st == null) continue;
            MenuItem item = new MenuItem(st.getName() + " [" + st.getCode() + "]");
            item.setOnAction(e -> {
                row.setShift(dayNum, st);
                hasUnsavedChanges = true;
                scheduleTable.refresh();
                statusLabel.setText("Módosítva: " + row.getEmployeeName() + " (" + dayNum + ". nap -> " + st.getCode() + ")");
            });
            menu.getItems().add(item);
        }
        return menu;
    }

    private void loadSchedulesFromDatabase() {
        tableRows.clear();
        List<User> users = userDAO.getAllUsers();
        List<WorkSchedule> dbSchedules = scheduleDAO.getSchedulesForMonth(
                currentYearMonth.getYear(), 
                currentYearMonth.getMonthValue()
        );

        Map<Integer, DailyScheduleRow> rowMap = new LinkedHashMap<>();
        for (User u : users) {
            DailyScheduleRow row = new DailyScheduleRow(u);
            rowMap.put(u.getId(), row);
            tableRows.add(row);
        }

        for (WorkSchedule ws : dbSchedules) {
            DailyScheduleRow row = rowMap.get(ws.getUser().getId());
            if (row != null) {
                row.setShift(ws.getDate().getDayOfMonth(), ws.getShiftType());
            }
        }

        hasUnsavedChanges = false;
        scheduleTable.refresh();
        statusLabel.setText("Adatok betöltve: " + dbSchedules.size() + " beosztás bejegyzés.");
    }

    @FXML
    private void handlePrevMonth(ActionEvent event) {
        loadMonthData(currentYearMonth.minusMonths(1));
    }

    @FXML
    private void handleNextMonth(ActionEvent event) {
        loadMonthData(currentYearMonth.plusMonths(1));
    }

    @FXML
    private void handleCurrentMonth(ActionEvent event) {
        loadMonthData(AppClock.currentMonth());
    }

    /**
     * Automatikus tervezés gomb (N–É–P–P rotáció és szabadságok kalkulációja)
     */
    @FXML
    private void handleAutoSchedule(ActionEvent event) {
        statusLabel.setText("Automatikus tervezés folyamatban...");
        List<WorkSchedule> generated = autoSchedulerService.generateMonthlySchedule(
                currentYearMonth.getYear(), 
                currentYearMonth.getMonthValue()
        );

        Map<Integer, DailyScheduleRow> rowMap = new HashMap<>();
        for (DailyScheduleRow row : tableRows) {
            rowMap.put(row.getUser().getId(), row);
        }

        for (WorkSchedule ws : generated) {
            DailyScheduleRow row = rowMap.get(ws.getUser().getId());
            if (row != null) {
                row.setShift(ws.getDate().getDayOfMonth(), ws.getShiftType());
            }
        }

        hasUnsavedChanges = true;
        scheduleTable.refresh();

        // Mt. Törvényi validáció (dolgozónként) és műszaklétszám-ellenőrzés a generált tervre
        MtLaborRuleValidator.ValidationResult result = laborRuleValidator.validateAll(generated);
        int minCrew = staffingService.getMinCrewPerShift();
        List<StaffingService.Shortage> shortages = StaffingService.findShortages(generated, minCrew);

        if (result.isValid() && shortages.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sikeres Automatikus Tervezés");
            alert.setHeaderText("A 4 műszakos N–É–P–P beosztás sikeresen legenerálva!");
            alert.setContentText("A beosztás maradéktalanul megfelel a Munka Törvénykönyve (Mt.) előírásainak " +
                    "(11 órás pihenőidők, 12 órás korlátok és jóváhagyott távollétek betartva), " +
                    "és minden műszakban megvan a minimális " + minCrew + " fős létszám.\n\nA tervet a „Mentés” gombbal mentheti el.");
            alert.showAndWait();
        } else {
            StringBuilder sb = new StringBuilder();
            if (!result.isValid()) {
                sb.append("MUNKAJOGI SZABÁLYÜTKÖZÉSEK:\n").append(result.getErrorMessage()).append("\n\n");
            }
            if (!shortages.isEmpty()) {
                sb.append("LÉTSZÁMHIÁNY – a jóváhagyott távollétek miatt az alábbi műszakokban nincs meg a minimális ")
                  .append(minCrew).append(" fő:\n")
                  .append(StaffingService.format(shortages, 40))
                  .append("\nTeendő: jobb klikkel osszon be helyettesítőt egy pihenőnapos dolgozóból, ")
                  .append("majd a „Mentés” gombbal mentse a tervet (a mentés ellenőrzi a pihenőidőt is).");
            }
            showReport(Alert.AlertType.WARNING, "Tervezési Figyelmeztetés",
                    "A generált beosztás ellenőrzést igényel:", sb.toString().trim());
        }
        statusLabel.setText("Automatikus beosztás elkészült (" + generated.size() + " műszak hozzárendelve).");
    }

    /**
     * Mentés gomb: munkajogi és létszám-ellenőrzés után a havi terv adatbázisba írása.
     */
    @FXML
    private void handleSave(ActionEvent event) {
        List<WorkSchedule> toSave = new ArrayList<>();
        int year = currentYearMonth.getYear();
        int month = currentYearMonth.getMonthValue();
        int days = currentYearMonth.lengthOfMonth();

        for (DailyScheduleRow row : tableRows) {
            for (int day = 1; day <= days; day++) {
                ShiftType shift = row.getShift(day);
                if (shift != null) {
                    toSave.add(new WorkSchedule(0, row.getUser(), shift, LocalDate.of(year, month, day), null));
                }
            }
        }

        // Mentés előtti munkajogi ellenőrzés (Mt. 99. §, Mt. 104. §) – szabálysértő terv nem menthető
        MtLaborRuleValidator.ValidationResult validation = laborRuleValidator.validateAll(toSave);
        if (!validation.isValid()) {
            showReport(Alert.AlertType.ERROR, "Mentés Megtagadva",
                    "A beosztás munkajogi szabályba ütközik, ezért nem menthető!",
                    validation.getErrorMessage() + "\n\nJavítsa a jelölt napokat (jobb klikk a cellán), majd mentsen újra.");
            statusLabel.setText("Mentés megtagadva: munkajogi szabálysértés.");
            return;
        }

        int minCrew = staffingService.getMinCrewPerShift();
        List<StaffingService.Shortage> shortages = StaffingService.findShortages(toSave, minCrew);
        if (!shortages.isEmpty()) {
            Optional<ButtonType> answer = showReport(Alert.AlertType.CONFIRMATION, "Létszámhiány",
                    "A tervben még vannak a minimális " + minCrew + " fő alatti műszakok. Mégis menti?",
                    StaffingService.format(shortages, 40), ButtonType.OK, ButtonType.CANCEL);
            if (answer.isEmpty() || answer.get() != ButtonType.OK) {
                return;
            }
        }

        try {
            scheduleDAO.saveSchedulesBatch(toSave);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "A mentés nem sikerült, az adatbázis változatlan: " + e.getMessage()).showAndWait();
            return;
        }
        markScenarioModified();
        hasUnsavedChanges = false;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sikeres Mentés");
        alert.setHeaderText("Beosztás elmentve");
        alert.setContentText("A " + currentYearMonth.getYear() + ". " + 
                currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("hu", "HU")) + 
                " havi beosztás sikeresen elmentve az adatbázisba (" + toSave.size() + " tétel).");
        alert.showAndWait();

        statusLabel.setText("Beosztás sikeresen elmentve.");
    }

    /**
     * PDF export: a mentett havi beosztás nyomtatható, aláírható dokumentumként (Mt. 97. §).
     * Csak mentett terv exportálható, mert a mentés előtt fut le a munkajogi ellenőrzés.
     */
    @FXML
    private void handleExportPdf(ActionEvent event) {
        if (hasUnsavedChanges) {
            new Alert(Alert.AlertType.WARNING, "A rácson mentetlen módosítások vannak. Előbb mentse a beosztást, "
                    + "mert csak a munkajogi ellenőrzésen átment, mentett terv exportálható.").showAndWait();
            return;
        }
        boolean empty = tableRows.stream().allMatch(r -> r.getDailyShifts().isEmpty());
        if (empty) {
            new Alert(Alert.AlertType.WARNING, "Ebben a hónapban még nincs mentett beosztás. "
                    + "Készítse el az „Automatikus Tervezés” gombbal, majd mentse.").showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Havi beosztás exportálása PDF-be");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF dokumentum", "*.pdf"));
        chooser.setInitialFileName(String.format("beosztas_%d_%02d.pdf",
                currentYearMonth.getYear(), currentYearMonth.getMonthValue()));
        File file = chooser.showSaveDialog(scheduleTable.getScene().getWindow());
        if (file == null) return;

        try (OutputStream out = new FileOutputStream(file)) {
            SchedulePdfExporter.export(currentYearMonth, new ArrayList<>(tableRows), AppClock.today(), out);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "A PDF exportálása nem sikerült: " + e.getMessage()).showAndWait();
            return;
        }
        statusLabel.setText("Havi beosztás exportálva: " + file.getName());
    }

    @FXML private void handleNavSchedule(ActionEvent event) { loadMonthData(currentYearMonth); }

    @FXML private void handleNavAnnual(ActionEvent event) {
        showAnnualLeaveAndRequestsDialog();
    }

    /**
     * Éves Óranyilvántartás és Szabadságkeret Kezelő Dialógus (Mt. 115-126. §).
     */
    private void showAnnualLeaveAndRequestsDialog() {
        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.setTitle("Éves nyilvántartás – szabadság, távollétek, órakontó");
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(15);
        root.setPadding(new javafx.geometry.Insets(20));
        root.setStyle("-fx-background-color: #f7fafc; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // Fejléc
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label title = new javafx.scene.control.Label(
                "Éves nyilvántartás – " + currentYearMonth.getYear() + ". év"
        );
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Button addRequestBtn = new javafx.scene.control.Button("➕ Új Távollét / Szabadság Rögzítése");
        addRequestBtn.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");

        header.getChildren().addAll(title, spacer, addRequestBtn);

        // Információs sáv
        javafx.scene.control.Label infoLabel = new javafx.scene.control.Label(
                "Törvényi alapok: Mt. 115-120. § (Alap- és pótszabadság), Mt. 122. § (15 napos előzetes bejelentés), Mt. 126. § (15 nap betegszabadság, felette táppénz), Mt. 134. § (Nyilvántartás).\n" +
                "Napok: munkanapban (H–P). Órakontó = beosztott órák + szabadság/betegség miatt kiesett műszakórák jóváírása − éves norma (munkanapok × 8 óra)."
        );
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096; -fx-font-style: italic;");

        // Éves egyenleg táblázat
        TableView<AnnualLeaveBalance> table = new TableView<>();
        table.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 6;");

        TableColumn<AnnualLeaveBalance, String> colName = new TableColumn<>("Dolgozó");
        colName.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getEmployeeName()));
        colName.setPrefWidth(160);

        TableColumn<AnnualLeaveBalance, String> colTeam = new TableColumn<>("Brigád");
        colTeam.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTeamName()));
        colTeam.setPrefWidth(85);

        TableColumn<AnnualLeaveBalance, Integer> colAge = new TableColumn<>("Kor");
        colAge.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getUser().getAge()));
        colAge.setPrefWidth(55);

        TableColumn<AnnualLeaveBalance, Integer> colTotalVac = new TableColumn<>("Éves Keret\n(Mt. 115-120)");
        colTotalVac.setCellValueFactory(d -> d.getValue().totalVacationDaysProperty().asObject());
        colTotalVac.setPrefWidth(100);

        TableColumn<AnnualLeaveBalance, Integer> colUsedVac = new TableColumn<>("Kivett Szabi\n(nap)");
        colUsedVac.setCellValueFactory(d -> d.getValue().usedVacationDaysProperty().asObject());
        colUsedVac.setPrefWidth(90);

        TableColumn<AnnualLeaveBalance, Integer> colRemVac = new TableColumn<>("Hátralévő Szabi\n(nap)");
        colRemVac.setCellValueFactory(d -> d.getValue().remainingVacationDaysProperty().asObject());
        colRemVac.setPrefWidth(110);
        colRemVac.setCellFactory(col -> new TableCell<AnnualLeaveBalance, Integer>() {
            @Override
            protected void updateItem(Integer rem, boolean empty) {
                super.updateItem(rem, empty);
                if (empty || rem == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(rem + " nap");
                    setAlignment(javafx.geometry.Pos.CENTER);
                    if (rem > 5) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #276749; -fx-background-color: #c6f6d5;");
                    } else if (rem > 0) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #c05621; -fx-background-color: #feebc8;");
                    } else {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #9b2c2c; -fx-background-color: #fed7d7;");
                    }
                }
            }
        });

        TableColumn<AnnualLeaveBalance, Integer> colSick = new TableColumn<>("Betegszabi\n(Mt. 126. §)");
        colSick.setCellValueFactory(d -> d.getValue().sickLeaveDaysProperty().asObject());
        colSick.setPrefWidth(95);
        colSick.setCellFactory(col -> new TableCell<AnnualLeaveBalance, Integer>() {
            @Override
            protected void updateItem(Integer days, boolean empty) {
                super.updateItem(days, empty);
                if (empty || days == null) {
                    setText(null);
                } else {
                    setText(days + " / 15 nap");
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });

        TableColumn<AnnualLeaveBalance, Integer> colSickPay = new TableColumn<>("Táppénz\n(15 nap felett)");
        colSickPay.setCellValueFactory(d -> d.getValue().sickPayDaysProperty().asObject());
        colSickPay.setPrefWidth(100);
        colSickPay.setCellFactory(col -> new TableCell<AnnualLeaveBalance, Integer>() {
            @Override
            protected void updateItem(Integer days, boolean empty) {
                super.updateItem(days, empty);
                if (empty || days == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(days > 0 ? days + " nap" : "-");
                    setAlignment(javafx.geometry.Pos.CENTER);
                    if (days > 0) {
                        setStyle("-fx-text-fill: #c53030; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        TableColumn<AnnualLeaveBalance, Integer> colUnpaid = new TableColumn<>("Fiz. nélküli\n(nap)");
        colUnpaid.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getUnpaidLeaveDays()));
        colUnpaid.setPrefWidth(85);
        colUnpaid.setCellFactory(col -> new TableCell<AnnualLeaveBalance, Integer>() {
            @Override
            protected void updateItem(Integer days, boolean empty) {
                super.updateItem(days, empty);
                if (empty || days == null) {
                    setText(null);
                } else {
                    setText(days > 0 ? days + " nap" : "-");
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });

        TableColumn<AnnualLeaveBalance, Double> colScheduledHours = new TableColumn<>("Beosztott\nÓrák");
        colScheduledHours.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getScheduledHours()));
        colScheduledHours.setPrefWidth(90);
        colScheduledHours.setCellFactory(col -> new TableCell<AnnualLeaveBalance, Double>() {
            @Override
            protected void updateItem(Double hrs, boolean empty) {
                super.updateItem(hrs, empty);
                if (empty || hrs == null) setText(null);
                else {
                    setText(String.format("%.1f h", hrs));
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });

        TableColumn<AnnualLeaveBalance, Double> colCredit = new TableColumn<>("Távollét\njóváírás");
        colCredit.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getAbsenceCreditHours()));
        colCredit.setPrefWidth(85);
        colCredit.setCellFactory(col -> new TableCell<AnnualLeaveBalance, Double>() {
            @Override
            protected void updateItem(Double hrs, boolean empty) {
                super.updateItem(hrs, empty);
                if (empty || hrs == null) setText(null);
                else {
                    setText(String.format("%.1f h", hrs));
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });

        TableColumn<AnnualLeaveBalance, Double> colBalance = new TableColumn<>("Keretegyenleg\n(Órakontó)");
        colBalance.setCellValueFactory(d -> d.getValue().hourBalanceProperty().asObject());
        colBalance.setPrefWidth(110);
        colBalance.setCellFactory(col -> new TableCell<AnnualLeaveBalance, Double>() {
            @Override
            protected void updateItem(Double bal, boolean empty) {
                super.updateItem(bal, empty);
                if (empty || bal == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%+.1f h", bal));
                    setAlignment(javafx.geometry.Pos.CENTER);
                    if (bal > 0) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #2c5282;");
                    } else if (bal < 0) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #c53030;");
                    } else {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");
                    }
                }
            }
        });

        table.getColumns().addAll(colName, colTeam, colAge, colTotalVac, colUsedVac, colRemVac, colSick, colSickPay, colUnpaid, colScheduledHours, colCredit, colBalance);
        javafx.scene.layout.VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        // Adatok betöltése a szolgáltatásból
        AnnualTimeTrackingService annualService = new AnnualTimeTrackingService();
        Runnable refreshTable = () -> {
            List<AnnualLeaveBalance> balances = annualService.calculateAnnualBalances(currentYearMonth.getYear());
            table.setItems(FXCollections.observableArrayList(balances));
        };
        refreshTable.run();

        // Új kérelem rögzítése akció
        addRequestBtn.setOnAction(e -> {
            showNewLeaveRequestDialog(dialogStage, annualService, refreshTable);
        });

        root.getChildren().addAll(header, infoLabel, table);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 1260, 620);
        dialogStage.setScene(scene);
        dialogStage.show();
    }

    /**
     * Új Szabadság / Betegszabadság Rögzítése űrlap törvényi validációval.
     */
    private void showNewLeaveRequestDialog(javafx.stage.Stage parentStage, AnnualTimeTrackingService annualService, Runnable onSaveSuccess) {
        javafx.stage.Stage formStage = new javafx.stage.Stage();
        formStage.setTitle("Új távollét rögzítése");
        formStage.initOwner(parentStage);
        formStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        javafx.scene.control.ComboBox<User> userCombo = new javafx.scene.control.ComboBox<>();
        userCombo.setItems(FXCollections.observableArrayList(userDAO.getAllUsers()));
        userCombo.setPromptText("Válassz dolgozót...");
        userCombo.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.Label quotaLabel = new javafx.scene.control.Label("Keret: -");
        quotaLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");

        userCombo.setOnAction(e -> {
            User u = userCombo.getValue();
            if (u != null) {
                int total = u.calculateTotalAnnualVacationDays(currentYearMonth.getYear());
                quotaLabel.setText("Éves szabadságkeret: " + total + " nap (Alap: 20, Életkori: " + 
                        u.calculateAgeBonusDays(currentYearMonth.getYear()) + ", Gyermekek: " + u.calculateChildrenBonusDays() + ")");
            }
        });

        javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(AppClock.today());
        javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(AppClock.today());

        javafx.scene.control.ComboBox<String> typeCombo = new javafx.scene.control.ComboBox<>();
        typeCombo.getItems().addAll("SZABADSAG", "BETEGSZABADSAG", "FIZETETLEN");
        typeCombo.setValue("SZABADSAG");

        javafx.scene.control.TextField reasonField = new javafx.scene.control.TextField();
        reasonField.setPromptText("Indoklás / Megjegyzés...");

        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("💾 Jóváhagyás & Rögzítés");
        saveBtn.setStyle("-fx-background-color: #38a169; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");

        grid.add(new javafx.scene.control.Label("Dolgozó:"), 0, 0);
        grid.add(userCombo, 1, 0);
        grid.add(quotaLabel, 1, 1);

        grid.add(new javafx.scene.control.Label("Távollét Típusa:"), 0, 2);
        grid.add(typeCombo, 1, 2);

        grid.add(new javafx.scene.control.Label("Kezdő dátum:"), 0, 3);
        grid.add(startDatePicker, 1, 3);

        grid.add(new javafx.scene.control.Label("Záró dátum:"), 0, 4);
        grid.add(endDatePicker, 1, 4);

        grid.add(new javafx.scene.control.Label("Indoklás:"), 0, 5);
        grid.add(reasonField, 1, 5);

        grid.add(saveBtn, 1, 6);

        saveBtn.setOnAction(e -> {
            User selectedUser = userCombo.getValue();
            if (selectedUser == null) {
                new Alert(Alert.AlertType.ERROR, "Kérlek válassz ki egy dolgozót!").showAndWait();
                return;
            }

            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            String type = typeCombo.getValue();
            String reason = reasonField.getText().trim();

            // Mt. Törvényi validáció futtatása
            AnnualTimeTrackingService.LeaveValidationResult val = annualService.validateAndCheckLeaveRequest(
                    selectedUser, start, end, type, start != null ? start.getYear() : currentYearMonth.getYear()
            );

            if (!val.isValid()) {
                showReport(formStage, Alert.AlertType.ERROR, "Szabálysértés Megtagadva",
                        "A távolléti kérelem nem rögzíthető!", val.getSummaryMessage());
                return;
            }

            if (val.hasConflicts()) {
                ButtonType reject = new ButtonType("Elutasítás", ButtonBar.ButtonData.CANCEL_CLOSE);
                ButtonType override = new ButtonType("Jóváhagyás mégis (helyettesítést szervezek)", ButtonBar.ButtonData.OK_DONE);
                Optional<ButtonType> opt = showReport(formStage, Alert.AlertType.WARNING, "Létszámhiány – Elutasítás Javasolt",
                        "A kérelem jóváhagyása esetén a brigád létszáma a minimum alá esne!",
                        val.getSummaryMessage(), reject, override);
                if (opt.isEmpty() || opt.get() != override) {
                    statusLabel.setText("Kérelem elutasítva létszámhiány miatt: " + selectedUser.getFullName());
                    return;
                }
            } else if (!val.getWarnings().isEmpty()) {
                Optional<ButtonType> opt = showReport(formStage, Alert.AlertType.CONFIRMATION, "Munkajogi Figyelmeztetés",
                        "Figyelmeztetés – kívánja ennek ellenére jóváhagyni?", val.getSummaryMessage(),
                        ButtonType.OK, ButtonType.CANCEL);
                if (opt.isEmpty() || opt.get() != ButtonType.OK) {
                    return;
                }
            }

            // Távollét rögzítése, majd a beosztás érintett napjainak átírása a távollét kódjára
            boolean success = timeOffDAO.save(selectedUser.getId(), start, end, type, reason);
            if (success) {
                success = applyTimeOffToSchedule(selectedUser, start, end, type);
            }

            if (success) {
                markScenarioModified();
                new Alert(Alert.AlertType.INFORMATION, "A távollét rögzítve, a beosztás frissítve.").showAndWait();
                formStage.close();
                onSaveSuccess.run();
                loadMonthData(currentYearMonth); // Fő táblázat azonnali frissítése
            } else {
                new Alert(Alert.AlertType.ERROR, "Hiba történt az adatbázis mentésekor!").showAndWait();
            }
        });

        javafx.scene.Scene formScene = new javafx.scene.Scene(grid, 550, 340);
        formStage.setScene(formScene);
        formStage.show();
    }

    /**
     * Demó helyzetek: életszerű esetek betöltése a program bemutatásához.
     */
    @FXML
    private void handleNavScenarios(ActionEvent event) {
        String previousId = activeScenario != null ? activeScenario.getId() : null;
        new ScenarioPickerDialog().show(scheduleTable.getScene().getWindow(),
                previousId,
                scenarioLoader.isActiveScenarioModified(),
                loaded -> {
                    activeScenario = loaded;
                    updateScenarioBanner();
                    loadMonthData(loaded.getFocusMonth());
                    statusLabel.setText((loaded.getId().equals(previousId) ? "Helyzet visszaállítva eredeti állapotba: "
                            : "Helyzet betöltve: ") + loaded.getDisplayName());
                    ScenarioPickerDialog.showDescription(scheduleTable.getScene().getWindow(), loaded);
                });
    }

    /** A demó helyzet módosítását jelzi (a fejlécben és a Demó helyzetek ablakban látszik). */
    private void markScenarioModified() {
        settingsDAO.markDemoModified();
        updateScenarioBanner();
    }

    @FXML
    private void handleShowScenarioInfo(ActionEvent event) {
        if (activeScenario != null) {
            ScenarioPickerDialog.showDescription(scheduleTable.getScene().getWindow(), activeScenario);
        }
    }

    @FXML
    private void handleNavStats(ActionEvent event) {
        SurchargeCalculator calc = new SurchargeCalculator();
        List<WorkSchedule> schedules = scheduleDAO.getSchedulesForMonth(currentYearMonth.getYear(), currentYearMonth.getMonthValue());
        SurchargeCalculator.MonthlySummary sum = calc.calculate(schedules);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Havi Összesítő Statisztika");
        alert.setHeaderText("Összesített munkaóra és pótlék kimutatás (" + currentYearMonth + ")");
        alert.setContentText(String.format(
                "• Beosztott munkaórák: %.2f óra\n" +
                "• 30%%-os Műszakpótlékos órák (18-06): %.2f óra\n" +
                "• Vasárnapi órák: %.2f óra\n" +
                "• Munkaszüneti napi órák (100%%): %.2f óra\n" +
                "• Dolgozói műszakok száma: %d db",
                sum.totalWorkHours, sum.shiftSurchargeHours, sum.sundayHours, sum.holidayHours, sum.workingDaysCount
        ));
        alert.showAndWait();
    }

    /**
     * A rögzített távollét napjain a beosztásba a távollét kódja kerül (SZ, B vagy F),
     * így a havi rács és az összesítések azonnal követik a változást.
     */
    private boolean applyTimeOffToSchedule(User user, LocalDate start, LocalDate end, String type) {
        ShiftType leaveShift = shiftTypeMap.get(AutoSchedulerService.leaveShiftCodeFor(type));
        List<WorkSchedule> entries = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            entries.add(new WorkSchedule(0, user, leaveShift, d, "Távollét"));
        }
        try {
            scheduleDAO.saveSchedulesBatch(entries);
            return true;
        } catch (Exception e) {
            System.err.println("Hiba a beosztás frissítésekor: " + e.getMessage());
            return false;
        }
    }

    private String getHungarianDayOfWeekShort(DayOfWeek dow) {
        switch (dow) {
            case MONDAY: return "H";
            case TUESDAY: return "K";
            case WEDNESDAY: return "Sze";
            case THURSDAY: return "Cs";
            case FRIDAY: return "P";
            case SATURDAY: return "Szo";
            case SUNDAY: return "V";
            default: return "";
        }
    }
}
