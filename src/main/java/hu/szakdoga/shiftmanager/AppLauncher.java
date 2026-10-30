package hu.szakdoga.shiftmanager;

import javafx.application.Application;

/**
 * Belépési pont az alkalmazás számára.
 * Nem terjeszti ki az Application osztályt, így elkerülhető az elterjedt IntelliJ / Java runtime hiba:
 * "JavaFX runtime components are missing, and are required to run this application".
 */
public class AppLauncher {
    public static void main(String[] args) {
        Application.launch(ShiftManagerApp.class, args);
    }
}