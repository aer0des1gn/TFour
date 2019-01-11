package de.tfour.main.java;

import processing.core.PApplet;
import processing.core.PFont;

public class Core extends PApplet {

    public static final int widthInTiles = 20;
    public static final int heightInTiles = 16;

    private Game game;

    public static void main(String... args) {
        PApplet.main("de.tfour.main.java.Core");
    }

    public void settings() {
        int width = (int) (widthInTiles * Tile.WIDTH);
        int height = (int) ((heightInTiles + 1.5f) * Tile.WIDTH);
        size(width, height);
    }

    public void setup() {
        PFont courierFont;
        courierFont = createFont("FSEX300.ttf", Tile.WIDTH);
        textFont(courierFont);
        textSize(Tile.WIDTH);
        noStroke();
        fill(0);
        game = new Game(this);
        game.setup();
    }

    public void draw() {
        game.draw();
    }

    public void keyPressed() {
        game.handleInput();
    }

    public void mousePressed() {
        game.handleInput();
    }

    public Game getGame() {
        return game;
    }

    //UTILITY

    public int getRandomColor() {
        return color(random(255), random(255), random(255));
    }

}