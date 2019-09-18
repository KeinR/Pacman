package net.keinr.pacman;

import javafx.scene.shape.Rectangle;

class Tile {
    double x, y;
    char type;
    Rectangle display;
    Tile(double x, double y, char type) {
        this.x = x;
        this.y = y;
        this.type = type;
        if (type == '1') {
            display = new Rectangle(Main.xScale*x, Main.yScale*y, Main.xScale, Main.yScale);
            Main.mapDisplay.getChildren().add(display);
        }
    }
}
