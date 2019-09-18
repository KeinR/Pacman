package net.keinr.pacman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.File;

import java.util.ArrayList;
import java.util.Random;

import javafx.application.Application;
// import javafx.application.Platform;

import javafx.stage.Stage;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class Main extends Application {

    private static final double width = 400;
    private static final double height = 400;

    private static Stage window;
    static Pane mapDisplay = new Pane();
    private static Pane sprites = new Pane();
    private static Pane root = new Pane();
    private static Scene scene;
    static double xScale, yScale;
    static Tile[][] map;
    static Random random = new Random();
    private static Timeline loop = new Timeline(new KeyFrame(Duration.millis((int)(1000/Main.frames_per_second)), e -> {

    }));

    @Override
    public void start(Stage stage) {
        window = stage;

        root.getChildren().addAll(mapDisplay, sprites);
        scene = new Scene(root, width, height);
        window.setScene(scene);

        ArrayList<String> map_al = new ArrayList<String>();

        // Load map

        try (BufferedReader br = new BufferedReader(new FileReader(new File("resources/map.txt")))) {
            String line;
            int yAxis = 0;
            while ((line = br.readLine()) != null) {
                map_al.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        map = new Tile[map_al.get(0).length()][map_al.size()];

        xScale = width/map.length;
        yScale = height/map[0].length;

        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[0].length; y++) {
                map[x][y] = new Tile(x, y, map_al.get(y).charAt(x));
            }
        }

        Player.init();

        scene.keyPressed(e -> {
            keys[e.getKeyCode()] = true;
        });
        ...
        scene.keyReleased (MouseEvent e) {
            keys[e.getKeyCode()] = false;
        }

        window.show();

    }









    public static void main(String[] args) { launch(args); }
}
