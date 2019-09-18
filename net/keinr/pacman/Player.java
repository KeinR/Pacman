package net.keinr.pacman;

import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;

class Player {
    static int x, y;
    static Rectangle display;
    static void move(int x, int y) {
        if (Main.map[x][y].type == '0') {

        }
    }

    static void init() {
        display = new Rectangle(
        Main.random.nextInt(Main.map.length) * Main.xScale,
        Main.random.nextInt(Main.map[0].length) * Main.yScale,
        Main.xScale,
        Main.yScale);
        display.setFill(Color.YELLOW);
        Main.mapDisplay.getChildren().add(display);
    }
}
