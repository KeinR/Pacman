package net.keinr.pacman;

import javafx.scene.input.KeyCode;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
// import javafx.scene.Node;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.List;
import java.util.Deque;
import java.util.ArrayList;
import java.util.ArrayDeque;

import net.keinr.util.TimedThread;
import static net.keinr.util.Ansi.YELLOW;

// Debug imports
import static net.keinr.util.Ansi.RED;
import static net.keinr.util.Ansi.MAGENTA;
import static net.keinr.util.Ansi.BLUE;
import static net.keinr.util.Ansi.RESET;
import static net.keinr.util.Ansi.BR_GREEN;
import static net.keinr.util.Ansi.BR_BLUE;
import static net.keinr.pacman.Main.logDebug;

class Engine {
    // Moddable constants
    private static final int TICK_INTERVAL = 10; // How often the game cycle runs, or "ticks", in milliseconds
    private static final double PLAYER_SPEED = 0.5; // How fast the player moves in pixels/tick
    private static final double GHOST_SPEED = 0.4; // How fast the ghosts moves in pixels/tick
    private static final double DOT_COLLECTION_DIST = 0.1; // Lower the value, the further away the player can pick up dots from. Should never be >=0.5
    private static final int GHOST_COUNT = 2; // # of ghosts. Doesn't depend on spawnpoint availability, as they can stack if there's overflow
    private static final int PATH_MEMORY = 3; // How many moves a ghost will remember after an A* run. Higher numbers will make it harder for the ghosts to track a moving player
    private static final int TRACKING_TIME = 20000; // How long a ghost will chase the player after spotting it (milliseconds)
    private static final Path SAVE_DATA_PATH = Paths.get(".pacman"); // Location where high scores will be saved
    private static final int POINTS_PER_DOT = 1; // How many points the player gets for each dot picked up
    private static final int POINTS_PER_SUPER_DOT = 10; // How many points the player gets for each super dot picked up
    private static final int POINTS_PER_GHOST = 50; // How many points the player gets for each ghost murdered
    private static final String MAP_SOURCE = "resources/map.png"; // Source of the map. If you choose to use an alternate one, make sure your color values are correct
    private static final Color DOT_COLOR = Color.YELLOW; // Normal dot color
    private static final Color DOT_COLOR_SUPER = Color.CYAN; // Super dot color
    private static final int JAW_SPEED = 5; // How fast the player's jaw opens and closes
    private static final int JAW_MAX = 40; // Max degrees the player's jaw can open
    private static final int SUPER_MODE_LENGTH = 10000; // How long a player's super mode lasts
    private static final int STAR_SPAWN_CHANCE = 5; // % chance that a star will spawn on a tile

    // Touch at your own risk
    private static final int WALL_DEC = 255; // Wall tile color representation in base 10
    private static final int PSP_DEC = 53504; // Player spawn point color representation in base 10
    private static final int ESP_DEC = 16711684; // Enemy spawn point color representation in base 10
    private static final int MAP_M = 20; // X/Y-length of tile matrix
    private static final double RATIO = 400/MAP_M; // Grid to pixels ratio
    private static final int PATHFINDING_ITER_CAP = 1000; // Absolute max A* iterations used to find a valid path to the target from ghost.
    private static final double ENTITY_RADIUS = RATIO/3; // radius of player & ghosts

    private static final TimedThread cycleControl = new TimedThread("cycle", TICK_INTERVAL, () -> cycle());
    private static final Random random = new Random();
    private static Tile[] enemySpawnpoints, playerSpawnpoints;
    private static Tile[][] map = new Tile[MAP_M][MAP_M];
    private static Tile[] openTiles;
    private static Ghost[] ghosts = new Ghost[GHOST_COUNT];
    private static Image ghostSprite, ghostEyesSprite, ghostScaredSprite;

    private static volatile boolean gameOver = true, paused = false;
    private static int score, highScore;

    // Player related stuff
    private static volatile KeyCode playerDirection, queuedPlayerDirection;
    private static final Arc playerDisplay = new Arc(-10, -10, ENTITY_RADIUS, ENTITY_RADIUS, 40, 300);
    private static boolean openingJaw = false;
    private static double mouthOpenSS = 10;
    private static int superModeDuration = 0;

    static void setup() {
        Interface.setOnKeyPressed(e -> {
            if (!gameOver) {
                if (paused) {
                    paused = false;
                    Interface.setUnpaused();
                } else if (playerDirection != e.getCode()) {
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
                            if (!paused) {
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

        // Load map
        try {
            final BufferedImage image = ImageIO.read(new FileInputStream(MAP_SOURCE));

            List<Tile> enemySpawnpointsPrototype = new ArrayList<Tile>();
            List<Tile> playerSpawnpointsPrototype = new ArrayList<Tile>();
            List<Tile> openTilesPrototype = new ArrayList<Tile>();

            for(int y = 0; y < map[0].length; y++) {
                for (int x = 0; x < map.length; x++) {
                    int value = image.getRGB(x, y) + 16777216; // Remove alpha value
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
                        openTilesPrototype.add(map[x][y]);
                    } else {
                        Rectangle rect = new Rectangle(x*RATIO, y*RATIO, RATIO, RATIO);
                        // String hex = Integer.toString(value, 16);
                        // hex = "0".repeat(6-hex.length())+hex;
                        rect.getStyleClass().add("wall");
                        Interface.addBackground(rect);
                    }
                }
            }
            enemySpawnpoints = enemySpawnpointsPrototype.toArray(Tile[]::new);
            playerSpawnpoints = playerSpawnpointsPrototype.toArray(Tile[]::new);
            openTiles = openTilesPrototype.toArray(Tile[]::new);

            // Add borders
            for (int x = 0; x < MAP_M; x++) {
                for (int y = 0; y < MAP_M; y++) {
                    if (map[x][y] != null) {
                        if (x+1 < MAP_M && map[x+1][y] == null) {
                            Rectangle rect = new Rectangle(x*RATIO+RATIO, y*RATIO, RATIO*0.1, RATIO);
                            rect.getStyleClass().add("wallBorder");
                            Interface.addBackground(rect);
                        }
                        if (x-1 >= 0 && map[x-1][y] == null) {
                            Rectangle rect = new Rectangle(x*RATIO-RATIO*0.1, y*RATIO, RATIO*0.1, RATIO);
                            rect.getStyleClass().add("wallBorder");
                            Interface.addBackground(rect);
                        }
                        if (y+1 < MAP_M && map[x][y+1] == null) {
                            Rectangle rect = new Rectangle(x*RATIO, y*RATIO+RATIO, RATIO, RATIO*0.1);
                            rect.getStyleClass().add("wallBorder");
                            Interface.addBackground(rect);
                        }
                        if (y-1 >= 0 && map[x][y-1] == null) {
                            Rectangle rect = new Rectangle(x*RATIO, y*RATIO-RATIO*0.1, RATIO, RATIO*0.1);
                            rect.getStyleClass().add("wallBorder");
                            Interface.addBackground(rect);
                        }
                    }
                }
            }

            ghostSprite = new Image(new FileInputStream("resources/sprites/ghost.png"));
            ghostEyesSprite = new Image(new FileInputStream("resources/sprites/eyes.png"));
            ghostScaredSprite = new Image(new FileInputStream("resources/sprites/scared.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize ghost array
        for (int i = 0; i < ghosts.length; i++) {
            ghosts[i] = new Ghost();
        }

        playerDisplay.setId("player");
        playerDisplay.setType(ArcType.ROUND);
        Interface.addEntity(playerDisplay);

        // Load save data
        try {
            highScore = Integer.parseInt(Files.readString(SAVE_DATA_PATH, StandardCharsets.UTF_8));
            Interface.setHighScore(highScore);
        } catch (NoSuchFileException e) {
            // It's fine, we'll make a new one later
        } catch (IOException e) {
            warn("Could not load save file: "+e.toString());
        } catch (NumberFormatException e) {
            warn("Save data is corrupted, and will be deleted");
            try {
                Files.delete(SAVE_DATA_PATH); // Remove corrupted save file so we don't encounter future errors
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        cycleControl.start();
    }

    static void start() {
        // Reset map
        for (int x = 0; x < MAP_M; x++) {
            for (int y = 0; y < MAP_M; y++) {
                if (map[x][y] != null) map[x][y].reset();
            }
        }

        // Spawn ghosts
        for (Ghost ghost : ghosts) ghost.spawn();

        // Spawn player
        Tile spawnpoint = playerSpawnpoints[random.nextInt(playerSpawnpoints.length)];
        map[spawnpoint.x][spawnpoint.y].setCollected(false);
        playerDisplay.setCenterX(spawnpoint.x*RATIO+RATIO/2);
        playerDisplay.setCenterY(spawnpoint.y*RATIO+RATIO/2);
        logDebug("Added player");

        playerDirection = KeyCode.P; // Starts paused
        Interface.setStartGame();

        score = 0;
        Interface.setScore(0);

        gameOver = false;
    }

    static void stop() {
        // Check protects against instances where two ghosts hit the player at the same time
        if (!gameOver) {
            Interface.setGameOver();
            save();
            logDebug("Game over, so sad...");
            gameOver = true;
        }
    }

    private static void addScore(int value) {
        score += value;
        Interface.setScore(score);
        if (score > highScore) {
            highScore = score;
            Interface.setHighScore(highScore);
        }
    }

    private static void save() {
        try {
            Files.write(SAVE_DATA_PATH, String.valueOf(highScore).getBytes());
        } catch (IOException e) {
            warn("Score autosave failed: "+e.toString());
        }
    }

    private static void warn(String message) {
        System.out.println("["+YELLOW+"warning"+RESET+"] "+message);
    }

    private static void cycle() {
        if (!paused && !gameOver) {

            final int x = (int)(playerDisplay.getCenterX()/RATIO);
            final int y = (int)(playerDisplay.getCenterY()/RATIO);
            double dist;
            boolean moved = false;
            switch (playerDirection) {
                case UP:
                    dist = playerDisplay.getCenterY()/RATIO - y;
                    if (y-1 >= 0 && (map[x][y-1] != null || dist > 0.5)) {
                        moved = true;
                        playerDisplay.setCenterY(playerDisplay.getCenterY()-PLAYER_SPEED);
                        if (dist > DOT_COLLECTION_DIST && y < MAP_M) {
                            map[x][y].setCollected(true);
                        }
                    } else recenterPlayer(x, y);
                    playerDisplay.setStartAngle(90+mouthOpenSS);
                    playerDisplay.setLength(360-(mouthOpenSS*2));
                    break;
                case DOWN:
                    dist = playerDisplay.getCenterY()/RATIO - y;
                    if (y+1 < MAP_M && (map[x][y+1] != null || dist < 0.5)) {
                        moved = true;
                        playerDisplay.setCenterY(playerDisplay.getCenterY()+PLAYER_SPEED);
                        if (dist > DOT_COLLECTION_DIST && y >= 0) {
                            map[x][y].setCollected(true);
                        }
                    } else recenterPlayer(x, y);
                    playerDisplay.setStartAngle(270+mouthOpenSS);
                    playerDisplay.setLength(360-(mouthOpenSS*2));
                    break;
                case LEFT:
                    dist = playerDisplay.getCenterX()/RATIO - x;
                    if (x-1 >= 0 && (map[x-1][y] != null || dist > 0.5)) {
                        moved = true;
                        playerDisplay.setCenterX(playerDisplay.getCenterX()-PLAYER_SPEED);
                        if (dist > DOT_COLLECTION_DIST && x < MAP_M) {
                            map[x][y].setCollected(true);
                        }
                    } else recenterPlayer(x, y);
                    playerDisplay.setStartAngle(180+mouthOpenSS);
                    playerDisplay.setLength(360-(mouthOpenSS*2));
                    break;
                case RIGHT:
                    dist = playerDisplay.getCenterX()/RATIO - x;
                    if (x+1 < MAP_M && (map[x+1][y] != null || dist < 0.5)) {
                        moved = true;
                        playerDisplay.setCenterX(playerDisplay.getCenterX()+PLAYER_SPEED);
                        if (dist > DOT_COLLECTION_DIST && x >= 0) {
                            map[x][y].setCollected(true);
                        }
                    } else recenterPlayer(x, y);
                    playerDisplay.setStartAngle(mouthOpenSS);
                    playerDisplay.setLength(360-(mouthOpenSS*2));
                    break;
                // case P: // Do nothing
            }
            if (moved) {
                if (mouthOpenSS < 0) {
                    openingJaw = true;
                    mouthOpenSS = 0;
                } else if (mouthOpenSS > JAW_MAX) {
                    openingJaw = false;
                    mouthOpenSS = JAW_MAX;
                } else if (openingJaw) {
                    mouthOpenSS += JAW_SPEED;
                } else {
                    mouthOpenSS -= JAW_SPEED;
                }
            } else {
                mouthOpenSS = JAW_MAX;
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
                        recenterPlayer(x, y);
                        playerDirection = queuedPlayerDirection;
                        queuedPlayerDirection = null;
                    } else {
                        logDebug("Queued move denied");
                    }
                } else {
                    // logDebug("Denied - ("+xx+", "+yy+")");
                }
            }
            for (Ghost ghost : ghosts) ghost.move();

            if (superModeDuration > 0) {
                superModeDuration -= TICK_INTERVAL;
            } else if (superModeDuration < 0) {
                exitSupermode();
                superModeDuration = 0;
            }
        }
    }

    private static void enterSupermode() {
        logDebug(BR_GREEN+"Entering SUPERMODE"+RESET);
        for (Ghost ghost : ghosts) {
            ghost.display.setScared();
            ghost.scared = true;
            ghost.moveQueue = new ArrayDeque<KeyCode>();
        }
        superModeDuration = SUPER_MODE_LENGTH;
    }

    private static void exitSupermode() {
        for (Ghost ghost : ghosts) {
            ghost.display.setNormal();
            ghost.scared = false;
        }
    }

    private static void recenterPlayer(int gridX, int gridY) {
        playerDisplay.setCenterX((gridX+0.5) * RATIO);
        playerDisplay.setCenterY((gridY+0.5) * RATIO);
    }

    private static class Tile {
        private final int x, y;
        private boolean collected = false;
        private final Circle display;
        private final int pointValue;
        private final boolean isPowerNode;
        private Tile(int x, int y) {
            this.x = x;
            this.y = y;
            this.display = new Circle(x*RATIO+RATIO/2, y*RATIO+RATIO/2, RATIO/6);
            if (random.nextInt(101) <= STAR_SPAWN_CHANCE) {
                this.pointValue = POINTS_PER_SUPER_DOT;
                this.display.setFill(DOT_COLOR_SUPER);
                this.isPowerNode = true;
            } else {
                this.pointValue = POINTS_PER_DOT;
                this.display.setFill(DOT_COLOR);
                this.isPowerNode = false;
            }
            Interface.add(this.display);
        }
        private void setCollected(boolean addScore) {
            if (!collected) {
                if (addScore) addScore(pointValue);
                collected = true;
                Interface.remove(display);
                if (isPowerNode) enterSupermode();
            }
        }
        private void reset() {
            collected = false;
            Interface.remove(this.display); // Making sure that the node isn't already listed
            Interface.add(this.display);
        }
    }

    private static class Ghost {
        private Deque<KeyCode> moveQueue;
        private KeyCode currentDirection;
        private final GhostSprite display = new GhostSprite();
        private boolean alive, changeDirClear, scared;
        private int trackingTime;
        private Tile trackedRandomTile, fleeLocation;
        private Ghost() {}

        private void spawn() {
            Tile spawnpoint = enemySpawnpoints[random.nextInt(enemySpawnpoints.length)];
            map[spawnpoint.x][spawnpoint.y].setCollected(false);
            display.setCenterX(spawnpoint.x*RATIO+RATIO/2);
            display.setCenterY(spawnpoint.y*RATIO+RATIO/2);
            alive = true;
            changeDirClear = true;
            scared = false;
            trackingTime = 0;
            trackedRandomTile = null;
            currentDirection = KeyCode.P;
            moveQueue = new ArrayDeque<KeyCode>();
        }

        private void move() {
            final int
            x = (int)(display.getCenterX()/RATIO),
            y = (int)(display.getCenterY()/RATIO), // Grid x/y values for ghost
            pxf = (int)(playerDisplay.getCenterX()/RATIO), // Player x/y grid values
            pyf = (int)(playerDisplay.getCenterY()/RATIO);
            final double speed = fleeLocation == null ? GHOST_SPEED : GHOST_SPEED*2;

            double dist;
            switch (currentDirection) {
                case UP:
                    dist = display.getCenterY()/RATIO - y;
                    if (y-1 >= 0 && (map[x][y-1] != null || dist >= 0.5)) {
                        display.setCenterY(display.getCenterY()-speed);
                    } else {
                        logDebug(RED+"UP denied"+RESET);
                        recenter(x, y);
                    }
                    break;
                case DOWN:
                    dist = display.getCenterY()/RATIO - y;
                    if (y+1 < MAP_M && (map[x][y+1] != null || dist <= 0.5)) {
                        display.setCenterY(display.getCenterY()+speed);
                    } else {
                        logDebug(RED+"DOWN denied"+RESET);
                        recenter(x, y);
                    }
                    break;
                case LEFT:
                    dist = display.getCenterX()/RATIO - x;
                    if (x-1 >= 0 && (map[x-1][y] != null || display.getCenterX()/RATIO - (int)(display.getCenterX()/RATIO) >= 0.5)) {
                        display.setCenterX(display.getCenterX()-speed);
                    } else {
                        logDebug(RED+"LEFT denied"+RESET);
                        recenter(x, y);
                    }
                    break;
                case RIGHT:
                    dist = display.getCenterX()/RATIO - x;
                    if (x+1 < MAP_M && (map[x+1][y] != null || dist <= 0.5)) {
                        display.setCenterX(display.getCenterX()+speed);
                    } else {
                        logDebug(RED+"RIGHT denied"+RESET);
                        recenter(x, y);
                    }
                    break;
                case P:
                    changeDirClear = true;
                    break;
            }

            // Check if ghost has completed a tile move
            double xx = display.getCenterX()/RATIO - x;
            double yy = display.getCenterY()/RATIO - y;
            if (xx > 0.485 && xx < 0.515 && yy > 0.485 && yy < 0.515) {

                if (changeDirClear && (x != pxf || y != pyf)) {

                    if (moveQueue.peek() != null) {
                        changeDirection(x, y);
                        // logDebug("Polled move |"+currentDirection+"|");
                    } else {

                        // Determine what to pathfind to
                        int px, py;
                        if (fleeLocation != null) {
                            px = fleeLocation.x;
                            py = fleeLocation.y;
                            if (x == fleeLocation.x && y == fleeLocation.y) {
                                fleeLocation = null;
                                display.setNormal();
                                alive = true;
                            }
                        } else if (scared) { // If fleeing due to player in super mode
                            logDebug(BR_GREEN+"--SCARED--"+RESET);

                            // Check 5 random tiles and flee to the one furthest from the player
                            Tile current, smallest = null;
                            int smallestDistance = -420; // Ha lol I did funny number lol I comedic genus ha ha
                            for (int i = 0; i < 5; i++) {
                                current = openTiles[random.nextInt(openTiles.length)];
                                int distance = Math.abs(current.x-pxf)+Math.abs(current.y-pyf);
                                if (distance > smallestDistance) {
                                    smallestDistance = distance;
                                    smallest = current;
                                }
                            }
                            trackedRandomTile = smallest;
                            logDebug(BLUE+"New flee location -> ("+trackedRandomTile.x+", "+trackedRandomTile.y+")"+RESET);

                            px = trackedRandomTile.x;
                            py = trackedRandomTile.y;
                        } else if (trackingTime <= 0) { // If the ghost has lost track of the player, have the ghost wander to a random tile
                            if (trackedRandomTile == null || (x == trackedRandomTile.x && y == trackedRandomTile.y)) {
                                // Search for an open random tile to wander to
                                trackedRandomTile = openTiles[random.nextInt(openTiles.length)];
                                logDebug(BLUE+"New target -> ("+trackedRandomTile.x+", "+trackedRandomTile.y+")"+RESET);
                            }
                            px = trackedRandomTile.x;
                            py = trackedRandomTile.y;
                            logDebug("Tracking random -> ("+px+", "+py+")");
                        } else {
                            px = pxf;
                            py = pyf;
                        }

                        logDebug(BR_GREEN+"Tracking time: "+trackingTime+RESET+"; Is scared = "+scared);

                        // Generate A* path

                        final List<Node> openList = new ArrayList<Node>();
                        boolean[][] closedMap = new boolean[MAP_M][MAP_M];
                        openList.add(new Node(null, x, y, 0, Math.abs(x-px)+Math.abs(y-px), null));
                        closedMap[x][y] = true;

                        Node result = null;

                        for (int iterations = 0; openList.size() > 0 && iterations < PATHFINDING_ITER_CAP; iterations++) {

                            // Find the (open) node with the lowest f value, and move it to the closed list
                            Node focus = openList.get(0);
                            for (Node node : openList) {
                                if (node.f < focus.f) {
                                    focus = node;
                                }
                            }
                            closedMap[focus.x][focus.y] = true;
                            openList.remove(focus);

                            if (focus.x == px && focus.y == py) { // Target aquired
                                result = focus;
                                logDebug(MAGENTA+"Found at ("+focus.x+", "+focus.y+") from ("+x+", "+y+") after "+iterations+" iterations"+RESET);
                                break;
                            }

                            // Gen chillren
                            // Ensure next tile is: in bounds, not a wall, and hasn't already been iterated over
                            int tScore = focus.c + 1;
                            if (focus.y-1 >= 0 && map[focus.x][focus.y-1] != null && !closedMap[focus.x][focus.y-1]) { // UP
                                boolean inOpenList = false;
                                for (Node node : openList) {
                                    if (node.x == focus.x && node.y == focus.y-1) {
                                        // Allow cheaper nodes to steal children from other nodes
                                        if (node.c < tScore) {
                                            node.setParent(focus);
                                        }
                                        inOpenList = true;
                                        break;
                                    }
                                }
                                if (!inOpenList) {
                                    openList.add(new Node(focus, focus.x, focus.y-1, 
                                    tScore,
                                    Math.abs(focus.x-px)+Math.abs(focus.y-1-px), KeyCode.UP));
                                }
                            }
                            if (focus.y+1 < MAP_M && map[focus.x][focus.y+1] != null && !closedMap[focus.x][focus.y+1]) { // DOWN
                                boolean inOpenList = false;
                                for (Node node : openList) {
                                    if (node.x == focus.x && node.y == focus.y+1) {
                                        if (node.c < tScore) {
                                            node.setParent(focus);
                                        }
                                        inOpenList = true;
                                        break;
                                    }
                                }
                                if (!inOpenList) {
                                    openList.add(new Node(focus, focus.x, focus.y+1,
                                    tScore,
                                    Math.abs(focus.x-px)+Math.abs(focus.y+1-px), KeyCode.DOWN));
                                    closedMap[focus.x][focus.y+1] = true;
                                }
                            }
                            if (focus.x-1 >= 0 && map[focus.x-1][focus.y] != null && !closedMap[focus.x-1][focus.y]) { // LEFT
                                boolean inOpenList = false;
                                for (Node node : openList) {
                                    if (node.x == focus.x-1 && node.y == focus.y) {
                                        if (node.c < tScore) {
                                            node.setParent(focus);
                                        }
                                        inOpenList = true;
                                        break;
                                    }
                                }
                                if (!inOpenList) {
                                    openList.add(new Node(focus, focus.x-1, focus.y, 
                                    tScore, 
                                    Math.abs(focus.x-1-px)+Math.abs(focus.y-px), KeyCode.LEFT));
                                    closedMap[focus.x-1][focus.y] = true;
                                }
                            }
                            if (focus.x+1 < MAP_M  && map[focus.x+1][focus.y] != null && !closedMap[focus.x+1][focus.y]) { // RIGHT
                                boolean inOpenList = false;
                                for (Node node : openList) {
                                    if (node.x == focus.x+1 && node.y == focus.y) {
                                        if (node.c < tScore) {
                                            node.setParent(focus);
                                        }
                                        inOpenList = true;
                                        break;
                                    }
                                }
                                if (!inOpenList) {
                                    openList.add(new Node(focus, focus.x+1, focus.y, 
                                    tScore, 
                                    Math.abs(focus.x+1-px)+Math.abs(focus.y-px), KeyCode.RIGHT));
                                    closedMap[focus.x+1][focus.y] = true;
                                }
                            }
                        }

                        // If we didn't reach the target before the iteration cap was hit, or there's no path, just get the "closets" one
                        if (result == null) {
                            logDebug(RED+"Launching contingency..."+RESET);
                            int minIndex = 0;
                            for (int i = 0; i < openList.size(); i++) {
                                if (openList.get(i).f < openList.get(minIndex).f) {
                                    minIndex = i;
                                }
                            }
                            result = openList.get(minIndex);
                        }

                        // Going backwards, get KeyCodes as list
                        List<KeyCode> sequence = new ArrayList<KeyCode>();
                        while (result != null && result.direction != null) {
                            sequence.add(result.direction);
                            result = result.parent;
                        }


                        // Going from the "start" of the path (the end of the sequence), record into memory
                        int cap;
                        if (trackingTime > 0) {
                            cap = sequence.size() - 1 - PATH_MEMORY;
                            if (cap <= 0) {
                                cap = sequence.size() - 2;
                                if (cap <= 0) {
                                    cap = 0;
                                }
                            }
                        } else {
                            cap = 0;
                        }
                        for (int i = sequence.size()-1; i >= cap; i--) {
                            // logDebug("Adding -> |"+sequence.get(i)+"|");
                            moveQueue.add(sequence.get(i));
                        }
                        // moveQueue.add(sequence.get(sequence.size()-1));

                        changeDirection(x, y);
                        logDebug("Polled move at END |"+currentDirection+"|");

                        logDebug("Found path; length = "+sequence.size()+" from ("+x+", "+y+") to ("+px+", "+py+")");
                    }
                }
            } else {
                changeDirClear = true;
            }

            if (trackingTime > 0) {
                trackingTime -= TICK_INTERVAL;
                // logDebug(RED+"Tracking player~"+trackingTime+RESET);
            }

            if (playerInView(x, y, pxf, pyf)) {
                if (trackingTime <= 0) {
                    logDebug(RED+"Path reset"+RESET);
                    moveQueue = new ArrayDeque<KeyCode>(); // Cancel queued path
                }
                trackingTime = TRACKING_TIME;
                // logDebug("+++++++PLAYER SPOTTED++++++++++");
            }
        }

        /**
         * @param x Ghost grid x
         * @param y Ghost grid y
         * @param px Player grid x
         * @param py Player grid y
         */
        private boolean playerInView(int x, int y, int px, int py) {
            if (x == px || y == py) {
                if (
                    Math.abs(display.getCenterX()-playerDisplay.getCenterX()) <= ENTITY_RADIUS+ENTITY_RADIUS && 
                    Math.abs(display.getCenterY()-playerDisplay.getCenterY()) <= ENTITY_RADIUS+ENTITY_RADIUS
                    ) { // Player/ghost collision
                    if (scared) {
                        display.setDead();
                        fleeLocation = enemySpawnpoints[random.nextInt(enemySpawnpoints.length)];
                        moveQueue = new ArrayDeque<KeyCode>();
                        alive = false;
                        scared = false;
                        logDebug(RED+"SET DEAD"+RESET);
                    } else if (alive) {
                        stop();
                    }
                }
                else if (x < px) {
                    for (int i = x; i < px; i++) {
                        if (map[i][y] == null) return false;
                    }
                } else if (x > px) {
                    for (int i = x; i > px; i--) {
                        if (map[i][y] == null) return false;
                    }
                } else if (y < py) {
                    for (int i = y; i < py; i++) {
                        if (map[x][i] == null) return false;
                    }
                } else/* if (y > py)*/ {
                    for (int i = y; i > py; i--) {
                        if (map[x][i] == null) return false;
                    }
                }
                return true;
            }
            return false;
        }

        private void changeDirection(int gridX, int gridY) {
            changeDirClear = false;
            currentDirection = moveQueue.poll();
            if (currentDirection == null) currentDirection = KeyCode.P; // P for paused
            recenter(gridX, gridY);
        }

        private void recenter(int gridX, int gridY) {
            display.setCenterX((gridX+0.5) * RATIO);
            display.setCenterY((gridY+0.5) * RATIO);
        }

        private static class Node {
            private Node parent;
            private final int x, y, h; // x, y, & heuristic
            private int c, f; // Cost & function
            private final KeyCode direction;
            private Node(Node parent, int x, int y, int c, int h, KeyCode direction) {
                this.parent = parent;
                this.x = x;
                this.y = y;
                this.c = c;
                this.h = h;
                this.f = this.c + this.h;
                this.direction = direction;
            }
            private void setParent(Node node) {
                this.c = node.c + 1;
                this.f = this.c + this.h;
                this.parent = node;
            }
        }

        private static class GhostSprite {
            private final ImageView base = new ImageView(ghostSprite);
            private final ImageView eyes = new ImageView(ghostEyesSprite);
            private final ImageView scared = new ImageView(ghostScaredSprite);
            private final ImageView[] list = { base, eyes, scared };
            private GhostSprite() {
                final double rad = ENTITY_RADIUS*2;
                base.setFitHeight(rad);
                base.setFitWidth(rad);
                eyes.setFitHeight(rad);
                eyes.setFitWidth(rad);
                scared.setFitHeight(rad);
                scared.setFitWidth(rad);
                Interface.addAllEntity(base, eyes);
            }
            private void setNormal() {
                reset();
                Interface.addAllEntity(base, eyes);
            }
            private void setScared() {
                reset();
                Interface.addAllEntity(base, scared);
            }
            private void setDead() {
                reset();
                Interface.addAllEntity(eyes);
            }
            private void reset() {
                Interface.removeAllEntity(base, eyes, scared);
            }
            private double getCenterX() {
                return base.getX()+ENTITY_RADIUS;
            }
            private double getCenterY() {
                return base.getY()+ENTITY_RADIUS;
            }
            private void setCenterX(double value) {
                for (ImageView image : list) image.setX(value-ENTITY_RADIUS);
            }
            private void setCenterY(double value) {
                for (ImageView image : list) image.setY(value-ENTITY_RADIUS);
            }
        }
    }
}
