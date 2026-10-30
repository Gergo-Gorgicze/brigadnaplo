package hu.szakdoga.shiftmanager.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * "Demó helyzetek" ablak: listázza a bemutatható életszerű helyzeteket, megmutatja a leírásukat,
 * és betölti a kiválasztottat a futó programba.
 */
public class ScenarioPickerDialog {

    private final ScenarioLoader loader = new ScenarioLoader();

    /**
     * @param owner          szülőablak
     * @param activeId       az éppen aktív helyzet azonosítója (vagy null)
     * @param activeModified igaz, ha az aktív helyzet adatait a betöltés óta módosították
     * @param onLoaded       sikeres betöltés vagy visszaállítás után hívódik (a főablak frissítéséhez)
     */
    public void show(Window owner, String activeId, boolean activeModified, Consumer<DemoScenario> onLoaded) {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("Demó helyzetek – életszerű esetek a program bemutatásához");
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);

        ListView<DemoScenario> list = new ListView<>();
        list.getItems().addAll(ScenarioCatalog.all());
        list.setPrefWidth(330);
        list.setCellFactory(lv -> new ListCell<DemoScenario>() {
            @Override
            protected void updateItem(DemoScenario s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                String marker = !s.getId().equals(activeId) ? "" : activeModified ? "   ● aktív (módosítva)" : "   ● aktív";
                Label title = new Label(s.getDisplayName() + marker);
                title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                title.setWrapText(true);
                Label desc = new Label(s.getShortDescription());
                desc.setWrapText(true);
                desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a5568;");
                VBox box = new VBox(3, title, desc);
                box.setPrefWidth(290);
                box.setMaxWidth(290);
                setGraphic(box);
                setText(null);
            }
        });

        TextArea details = new TextArea();
        details.setEditable(false);
        details.setWrapText(true);
        details.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        list.getSelectionModel().selectedItemProperty().addListener((obs, old, s) -> {
            details.setText(s != null ? s.getFullDescription() : "");
            details.positionCaret(0);
        });

        Label warning = new Label("Minden helyzet bármikor visszaállítható az eredeti állapotába: válaszd ki, és töltsd be újra. " +
                "A betöltés felülírja az adatbázisban lévő dolgozókat, beosztásokat és távolléteket.");
        warning.setWrapText(true);
        warning.setStyle("-fx-font-size: 11px; -fx-text-fill: #9b2c2c;");

        Button loadBtn = new Button("▶ Helyzet betöltése");
        // Az aktív helyzetnél a gomb visszaállításként jelenik meg
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, s) ->
                loadBtn.setText(s != null && s.getId().equals(activeId)
                        ? "↺ Visszaállítás eredeti állapotba" : "▶ Helyzet betöltése"));
        loadBtn.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        loadBtn.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());

        Button closeBtn = new Button("Bezárás");
        closeBtn.setOnAction(e -> stage.close());

        loadBtn.setOnAction(e -> {
            DemoScenario s = list.getSelectionModel().getSelectedItem();
            if (s == null) return;

            boolean reset = s.getId().equals(activeId);
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.initOwner(stage);
            confirm.setTitle(reset ? "Helyzet visszaállítása" : "Helyzet betöltése");
            confirm.setHeaderText(s.getDisplayName());
            confirm.setContentText(reset
                    ? "A helyzet visszaáll a kiinduló állapotába: a bemutatás közben rögzített távollétek és kézi módosítások törlődnek. Folytatod?"
                    : "A jelenlegi dolgozói, beosztási és távolléti adatok törlődnek, és a helyzet adatai kerülnek a helyükre. Folytatod?");
            Optional<ButtonType> answer = confirm.showAndWait();
            if (answer.isEmpty() || answer.get() != ButtonType.OK) return;

            try {
                loader.load(s);
                stage.close();
                onLoaded.accept(s);
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.initOwner(stage);
                err.setTitle("Betöltési hiba");
                err.setHeaderText("A helyzet betöltése nem sikerült, az adatbázis változatlan maradt.");
                err.setContentText(ex.getMessage());
                err.showAndWait();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, warning, spacer, closeBtn, loadBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(warning, Priority.SOMETIMES);
        // A gombok feliratát ne vágja le a hosszú figyelmeztető szöveg
        closeBtn.setMinWidth(Region.USE_PREF_SIZE);
        loadBtn.setMinWidth(Region.USE_PREF_SIZE);
        warning.setMinWidth(0);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setLeft(list);
        BorderPane.setMargin(list, new Insets(0, 12, 0, 0));
        root.setCenter(details);
        root.setBottom(buttons);
        BorderPane.setMargin(buttons, new Insets(12, 0, 0, 0));
        root.setStyle("-fx-background-color: #f7fafc; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // Az aktív (vagy az első) helyzet előválasztása
        DemoScenario preselect = ScenarioCatalog.byId(activeId);
        list.getSelectionModel().select(preselect != null ? preselect : ScenarioCatalog.all().get(0));

        stage.setScene(new Scene(root, 1100, 640));
        stage.show();
    }

    /** Az aktív helyzet leírásának megjelenítése (a főablak "ℹ Leírás" gombja). */
    public static void showDescription(Window owner, DemoScenario scenario) {
        TextArea text = new TextArea(scenario.getFullDescription());
        text.setEditable(false);
        text.setWrapText(true);
        text.setPrefSize(760, 520);
        text.setStyle("-fx-font-size: 13px;");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("Demó helyzet leírása");
        alert.setHeaderText(scenario.getDisplayName());
        alert.getDialogPane().setContent(text);
        alert.setResizable(true);
        alert.show();
    }
}
