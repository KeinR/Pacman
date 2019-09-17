package net.keinr.pacman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.File;

import java.util.ArrayList;

import javafx.application.Application;
// import javafx.application.Platform;

import javafx.stage.Stage;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

public class Main extends Application {

    private static final double width = 400;
    private static final double height = 400;

    private static Stage window;
    private static Pane mapDisplay = new Pane();
    private static Pane sprites = new Pane();
    private static Pane root = new Pane();

    @Override
    public void start(Stage stage) {
        window = stage;

        ArrayList<>

        // Load map

        try (BufferedReader br = new BufferedReader(new FileReader(new File("resources/map.txt")))) {
            String line;
            int yAxis = 0;
            while ((line = br.readLine()) != null) {
                int length = line.length();
                for (int i = 0; i < length; i++) {
                    if (line.charAt(i) == '0') {

                    } else {

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }






    public static void main(String[] args) { launch(args); }
}
