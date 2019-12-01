package net.keinr.pacman;


import javafx.stage.Stage;
import javafx.scene.Scene;
// import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.scene.shape.Rectangle;


import static net.keinr.pacman.Main.logDebug;

class Interface {

    // Text
    private static final Label
    highScore = new Label("High Score: 0"),
    score = new Label("Score: 0"),
    gameOverDisplay = new Label("GAME OVER"),
    gameOverDesc = new Label("Press any key to retry..."),
    pausedTitle = new Label("Paused"),
    pausedDesc = new Label("Press any key to continue...");

    // Roots
    private static final Pane
    backgroundRoot = new Pane(),
    gameRoot = new Pane(),
    entityRoot = new Pane(),
    gameOverRoot = new Pane(),
    pausedRoot = new Pane(),
    mainRoot = new Pane(),
    overlayRoot = new Pane(),
    masterRoot = new Pane();

    // Main scene
    private static final Scene main = new Scene(masterRoot, 400, 400);

    static void setup(Stage window) {

        Rectangle mainBackground = new Rectangle(0, 0, 400, 400);
        mainBackground.setId("mainBackground");

        Rectangle gameOverBackground = new Rectangle(0, 0, 400, 400);
        gameOverBackground.setId("gameOverBackground");

        gameOverDisplay.setLayoutX(200);
        gameOverDisplay.setLayoutY(200);
        gameOverDisplay.setId("gameOverDisplay");

        gameOverDesc.setLayoutX(200);
        gameOverDesc.setLayoutY(220);
        gameOverDesc.setId("gameOverDesc");

        Rectangle pausedBackground = new Rectangle(0, 0, 400, 400);
        pausedBackground.setId("pausedBackground");

        pausedTitle.setLayoutX(200);
        pausedTitle.setLayoutY(200);
        pausedTitle.setId("pausedTitle");

        pausedDesc.setLayoutX(200);
        pausedDesc.setLayoutY(220);
        pausedDesc.setId("pausedDesc");

        Rectangle topbarBackground = new Rectangle(0, 0, 400, 17);
        topbarBackground.setId("topbarBackground");

        Rectangle topbarBorder = new Rectangle(0, 17, 400, 3);
        topbarBorder.setId("topbarBorder");

        score.setLayoutX(5);
        score.setLayoutY(0);

        highScore.setLayoutX(270);
        highScore.setLayoutY(0);

        backgroundRoot.getChildren().add(mainBackground);

        gameOverRoot.getChildren().addAll(gameOverBackground, gameOverDisplay, gameOverDesc);

        pausedRoot.getChildren().addAll(pausedBackground, pausedTitle, pausedDesc);

        mainRoot.getChildren().addAll(backgroundRoot, gameRoot, entityRoot, gameOverRoot);

        overlayRoot.getChildren().addAll(topbarBackground, topbarBorder, score, highScore);

        masterRoot.getChildren().addAll(mainRoot, overlayRoot);

        main.getStylesheets().add("resources/css/main.css");

        window.setResizable(false);
        window.setScene(main);
        window.show();
    }

    static void add(Node node) {
        Platform.runLater(() -> gameRoot.getChildren().add(node));
    }

    static void addEntity(Node node) {
        Platform.runLater(() -> entityRoot.getChildren().add(node));
    }

    static void addBackground(Node node) {
        Platform.runLater(() -> backgroundRoot.getChildren().add(node));
    }

    /*
    static void reset() {
        Platform.runLater(() -> {
            main.getChildren().add(gameOverRoot);
            gameRoot.getChildren().clear();
        });
    }
    */

    static void setPaused() {
        Platform.runLater(() -> mainRoot.getChildren().add(pausedRoot));
    }

    static void setUnpaused() {
        Platform.runLater(() -> mainRoot.getChildren().remove(pausedRoot));
    }

    static void remove(Node node) {
        Platform.runLater(() -> gameRoot.getChildren().remove(node));
    }

    static void setScore(int value) {
        String val = String.valueOf(value);
        Platform.runLater(() -> score.setText("Score: "+val));
    }

    static void setHighScore(int value) {
        String val = String.valueOf(value);
        Platform.runLater(() -> highScore.setText("High Score: "+val));
    }

    static void setGameOver() {
        Platform.runLater(() -> mainRoot.getChildren().add(gameOverRoot));
    }

    static void setStartGame() {
        Platform.runLater(() -> mainRoot.getChildren().remove(gameOverRoot));
    }

    static void setOnKeyPressed(EventHandler<? super KeyEvent> eventHandle) {
        main.setOnKeyPressed(eventHandle);
        logDebug("set key press");
    }
}

