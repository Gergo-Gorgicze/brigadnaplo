package hu.szakdoga.shiftmanager;

import hu.szakdoga.shiftmanager.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ShiftManagerApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Betöltjük az FXML nézetet
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/hu/szakdoga/shiftmanager/view/schedule-view.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 780);

        primaryStage.setTitle("Brigádnapló – Négy műszakos óranyilvántartó és vezénylési rendszer");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);

        // Alkalmazás bezárásakor felszabadítjuk az adatbázis kapcsolatot
        primaryStage.setOnCloseRequest(event -> {
            DatabaseManager.shutdown();
        });

        primaryStage.show();
    }
}