package net.keinr.pacman;

import javafx.application.Application;
import javafx.stage.Stage;
import java.util.concurrent.Executors;

import net.keinr.util.Debug;

/**
 * Initialization (Main) class
 * @author Orion Musselman (KeinR)
 */

public class Main extends Application {
    private static final boolean DEBUG_ENABLED = false;
    
    private static final Debug debug = new Debug(DEBUG_ENABLED);

    @Override
    public void start(Stage stage) {
        if (DEBUG_ENABLED) {
            debug.setShorthand("^net\\.keinr\\.pacman\\.");
            debug.log("Debug enabled");
        }
        Interface.setup(stage);
        Executors.newSingleThreadExecutor().execute(() -> Engine.setup());
    }

    static void logDebug(String message) {
        debug.logRouted(message, 1);
    }

    static void fDebug(Runnable function) {
        debug.logFunctionRouted(function, 1);
    }

    public static void main(String[] args) { launch(args); }
}
