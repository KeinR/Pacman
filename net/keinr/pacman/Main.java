package net.keinr.pacman;

import javafx.application.Application;
import javafx.stage.Stage;
import java.util.concurrent.Executors;

import net.keinr.util.Debug;
import static net.keinr.util.Debug.logDebug;

public class Main extends Application {
    private static final boolean DEBUG_ENABLED = true;

    @Override
    public void start(Stage stage) {
        if (DEBUG_ENABLED) {
            Debug.setShorthand("^net\\.keinr\\.pacman\\.");
            logDebug("Debug enabled");
        } else {
            Debug.toggleDebug(false);
        }
        Interface.setup(stage);
        Executors.newSingleThreadExecutor().execute(() -> Engine.setup());
    }

    public static void main(String[] args) { launch(args); }
}
