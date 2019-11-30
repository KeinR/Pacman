package net.keinr.pacman;

import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
// import javafx.scene.Node;
import javafx.scene.shape.Circle;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.FileInputStream;

import java.util.Random;
import java.util.List;
import java.util.Deque;
import java.util.ArrayList;
import java.util.ArrayDeque;

import static net.keinr.util.Ansi.RED;
import static net.keinr.util.Ansi.RESET;

import static net.keinr.util.Debug.logDebug;

class Engine {
    // Moddable constants
    private static final int TICK_INTERVAL = 10; // How often the game cycle runs, or "ticks", in milliseconds
    private static final double PLAYER_SPEED = 0.3; // How fast the player moves in pixels/tick
    private static final double DOT_COLLECTION_DIST = 0.1; // Lower the value, the further away the player can pick up dots from. Should never be >=0.5

    // Touch at your own risk
    private static final int WALL_DEC = 255; // Wall tile color representation in base 10
    private static final int PSP_DEC = 53504; // Player spawn point color representation in base 10
    private static final int ESP_DEC = 16711684; // Enemy spawn point color representation in base 10
    private static final int MAP_M = 20; // X/Y-length of tile matrix
    private static final double RATIO = 400/MAP_M; // Grid to pixels ratio

    private static final Timeline cycleControl = new Timeline(new KeyFrame(Duration.millis(TICK_INTERVAL), e -> cycle()));
    private static final Random random = new Random();
    private static Tile[] enemySpawnpoints, playerSpawnpoints;
    private static Tile[][] map = new Tile[MAP_M][MAP_M];

    private static volatile boolean gameOver = true, paused = false;

    // Player related stuff
    private static volatile KeyCode playerDirection, queuedPlayerDirection;
    private static final Circle playerDisplay = new Circle();

    static void setup() {
        Interface.setOnKeyPressed(e -> {
            if (!gameOver) {
                if (playerDirection != e.getCode()) {
                    switch (e.getCode()) {
                        case UP:
                            if (playerDirection == KeyCode.DOWN) {
                                playerDirection = e.getCode();
                            } else {
                                queuedPlayerDirection = e.getCode();
                            }
                            break;
                        case DOWN:
                            if (playerDirection == KeyCode.UP) {
                                playerDirection = e.getCode();
                            } else {
                                queuedPlayerDirection = e.getCode();
                            }
                            break;
                        case LEFT:
                            if (playerDirection == KeyCode.RIGHT) {
                                playerDirection = e.getCode();
                            } else {
                                queuedPlayerDirection = e.getCode();
                            }
                            break;
                        case RIGHT:
                            if (playerDirection == KeyCode.LEFT) {
                                playerDirection = e.getCode();
                            } else {
                                queuedPlayerDirection = e.getCode();
                            }
                            break;
                        case P:
                            if (paused) {
                                paused = false;
                                Interface.setUnpaused();
                            } else {
                                paused = true;
                                Interface.setPaused();
                            }
                            break;
                        default: logDebug("Bad key");
                    }
                }
            } else {
                start();
            }
        });
        cycleControl.setCycleCount(Timeline.INDEFINITE);

        // Load map
        try {
            final BufferedImage image = ImageIO.read(new FileInputStream("resources/map.png"));

            List<Tile> enemySpawnpointsPrototype = new ArrayList<Tile>();
            List<Tile> playerSpawnpointsPrototype = new ArrayList<Tile>();

            for(int y = 0; y < map[0].length; y++) {
                for (int x = 0; x < map.length; x++) {
                    int value = image.getRGB(x, y) + 16777216;
                    if (value != WALL_DEC) {
                        map[x][y] = new Tile(x, y);
                        switch (value) { // Get sections
                            case ESP_DEC: // Enemy sp
                                enemySpawnpointsPrototype.add(map[x][y]);
                                break;
                            case PSP_DEC: // Player sp
                                playerSpawnpointsPrototype.add(map[x][y]);
                                break;
                        }
                    } else {
                        Rectangle rect = new Rectangle(x*RATIO, y*RATIO, RATIO, RATIO);
                        // String hex = Integer.toString(value, 16);
                        // hex = "0".repeat(6-hex.length())+hex;
                        rect.setFill(Color.BLUE);
                        Interface.addBackground(rect);
                    }
                }
            }
            enemySpawnpoints = enemySpawnpointsPrototype.toArray(Tile[]::new);
            playerSpawnpoints = playerSpawnpointsPrototype.toArray(Tile[]::new);

            // Add borders
            for (int x = 0; x < MAP_M; x++) {
                for (int y = 0; y < MAP_M; y++) {
                    if (map[x][y] != null) {
                        if (x+1 < MAP_M && map[x+1][y] == null) {
                            Rectangle rect = new Rectangle(x*RATIO+RATIO, y*RATIO, RATIO*0.1, RATIO);
                            rect.setFill(Color.BLACK);
                            Interface.addBackground(rect);
                        }
                        if (x-1 >= 0 && map[x-1][y] == null) {
                            Rectangle rect = new Rectangle(x*RATIO-RATIO*0.1, y*RATIO, RATIO*0.1, RATIO);
                            rect.setFill(Color.BLACK);
                            Interface.addBackground(rect);
                        }
                        if (y+1 < MAP_M && map[x][y+1] == null) {
                            Rectangle rect = new Rectangle(x*RATIO, y*RATIO+RATIO, RATIO, RATIO*0.1);
                            rect.setFill(Color.BLACK);
                            Interface.addBackground(rect);
                        }
                        if (y-1 >= 0 && map[x][y-1] == null) {
                            Rectangle rect = new Rectangle(x*RATIO, y*RATIO-RATIO*0.1, RATIO, RATIO*0.1);
                            rect.setFill(Color.BLACK);
                            Interface.addBackground(rect);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        playerDisplay.setRadius(RATIO/3);

        // start(); // TEMP
    }

    static void start() {
        for (int x = 0; x < MAP_M; x++) {
            for (int y = 0; y < MAP_M; y++) {
                if (map[x][y] != null) map[x][y].reset();
            }
        }

        Tile spawnpoint = playerSpawnpoints[random.nextInt(playerSpawnpoints.length)];

        map[spawnpoint.x][spawnpoint.y].setCollected();
        playerDisplay.setCenterX(spawnpoint.x*RATIO+RATIO/2);
        playerDisplay.setCenterY(spawnpoint.y*RATIO+RATIO/2);
        Interface.add(playerDisplay);
        logDebug("Added player");

        playerDirection = KeyCode.P; // Starts paused
        cycleControl.play();
        Interface.setStartGame();
        gameOver = false;
    }

    static void stop() {
        Interface.remove(playerDisplay);
        Interface.setGameOver();
        cycleControl.stop();
        logDebug("Game over, so sad...");
        gameOver = true;
    }

    private static void cycle() {
        if (!paused) {
            final int x = (int)(playerDisplay.getCenterX()/RATIO);
            final int y = (int)(playerDisplay.getCenterY()/RATIO);
            double dist;
            switch (playerDirection) {
                case UP:
                    dist = playerDisplay.getCenterY()/RATIO - y;
                    if (y-1 >= 0 && (map[x][y-1] != null || dist > 0.5)) {
                        playerDisplay.setCenterY(playerDisplay.getCenterY()-PLAYER_SPEED);
                        if (dist > DOT_COLLECTION_DIST && y < MAP_M) {
                            map[x][y].setCollected();
                        }
                    }
                    break;
                case DOWN:
                    dist = playerDisplay.getCenterY()/RATIO - y;
                    if (y+1 < MAP_M && (map[x][y+1] != null || dist < 0.5)) {
                        playerDisplay.setCenterY(playerDisplay.getCenterY()+PLAYER_SPEED);
                        if (dist > DOT_COLLECTION_DIST && y >= 0) {
                            map[x][y].setCollected();
                        }
                    }
                    break;
                case LEFT:
                    dist = playerDisplay.getCenterX()/RATIO - x;
                    if (x-1 >= 0 && (map[x-1][y] != null || playerDisplay.getCenterX()/RATIO - (int)(playerDisplay.getCenterX()/RATIO) > 0.5)) {
                        playerDisplay.setCenterX(playerDisplay.getCenterX()-PLAYER_SPEED);
                        if (dist > DOT_COLLECTION_DIST && x < MAP_M) {
                            map[x][y].setCollected();
                        }
                    }
                    break;
                case RIGHT:
                    dist = playerDisplay.getCenterX()/RATIO - x;
                    if (x+1 < MAP_M && (map[x+1][y] != null || dist < 0.5)) {
                        playerDisplay.setCenterX(playerDisplay.getCenterX()+PLAYER_SPEED);
                        if (dist > DOT_COLLECTION_DIST && x >= 0) {
                            map[x][y].setCollected();
                        }
                    }
                    break;
                // case P: // Do nothing
            }
            if (queuedPlayerDirection != null) {
                double xx = playerDisplay.getCenterX()/RATIO - x;
                double yy = playerDisplay.getCenterY()/RATIO - y;
                if (xx > 0.485 && xx < 0.515 && yy > 0.485 && yy < 0.515) {
                    boolean canChangeDirection = false;
                    switch (queuedPlayerDirection) {
                        case UP: canChangeDirection = y-1 >= 0 && map[x][y-1] != null; break;
                        case DOWN: canChangeDirection = y+1 < MAP_M && map[x][y+1] != null; break;
                        case LEFT: canChangeDirection = x-1 >= 0 && map[x-1][y] != null; break;
                        case RIGHT: canChangeDirection = x+1 < MAP_M && map[x+1][y] != null; break;
                    }
                    if (canChangeDirection) {
                        // Re-center & switch to next move
                        playerDisplay.setCenterX((x+0.5) * RATIO);
                        playerDisplay.setCenterY((y+0.5) * RATIO);
                        playerDirection = queuedPlayerDirection;
                        queuedPlayerDirection = null;
                    } else {
                        logDebug("Queued move denied");
                    }
                } else {
                    // logDebug("Denied - ("+xx+", "+yy+")");
                }
            }
        }
    }

    private static class Tile {
        private final int x, y;
        private boolean collected = false;
        private final Circle display;
        private Tile(int x, int y) {
            this.x = x;
            this.y = y;
            this.display = new Circle(x*RATIO+RATIO/2, y*RATIO+RATIO/2, RATIO/6);
            Interface.add(this.display);
        }
        private void setCollected() {
            this.collected = true;
            Interface.remove(display);
        }
        private void reset() {
            this.collected = false;
            Interface.remove(this.display); // Making sure that the node isn't already listed as a precaution against IllegalArgumentException
            Interface.add(this.display);
        }
        @Override
        protected Tile clone() {
            return new Tile(x, y);
        }
    }

    private static class Ghost {
        private double x, y;
        private final Deque<KeyCode> moveQueue = new ArrayDeque<KeyCode>();
        private KeyCode currentDirection;
        private Ghost() {
            Tile spawnpoint = playerSpawnpoints[random.nextInt(playerSpawnpoints.length)];
        }

        private void move() {

        }
    }
}
