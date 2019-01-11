package de.tfour.main.java;

import processing.core.PApplet;

public class Item {
    private Core core;

    private int x, y;
    private char model;
    private String name = "Potion";

    public static Item create(Core core, int x, int y, char model) {
        Item e = new Item(core, x, y, model);
        Tile tile = core.getGame().getTile(x, y);
        if (tile != null && tile.hasItemSpace()) {
            tile.addItem(e);
            return e;
        }
        return null;
    }

    protected Item(Core core, int x, int y, char model) {
        this.core = core;
        this.x = x;
        this.y = y;
        this.model = model;
    }

    public void draw(int pos, int total) {
        core.fill(255);
        if (total == 1) {
            core.ellipse(x * Tile.WIDTH + Tile.WIDTH * 0.5f, y * Tile.WIDTH + Tile.WIDTH * 0.5f, Tile.WIDTH * 0.5f, Tile.WIDTH * 0.5f);
            return;
        }
        float itemSize = Tile.WIDTH * 0.35f;
        switch (pos) {
            case 0:
                if (total == 2) {
                    core.ellipse(x * Tile.WIDTH + Tile.WIDTH * 0.25f, y * Tile.WIDTH + Tile.WIDTH * 0.5f, itemSize, itemSize);
                } else {
                    core.ellipse(x * Tile.WIDTH + Tile.WIDTH * 0.25f, y * Tile.WIDTH + Tile.WIDTH * 0.25f, itemSize, itemSize);
                }
                break;
            case 1:
                if (total == 2) {
                    core.ellipse(x * Tile.WIDTH + Tile.WIDTH * 0.75f, y * Tile.WIDTH + Tile.WIDTH * 0.5f, itemSize, itemSize);
                } else {
                    core.ellipse(x * Tile.WIDTH + Tile.WIDTH * 0.75f, y * Tile.WIDTH + Tile.WIDTH * 0.25f, itemSize, itemSize);
                }
                break;
            case 2:
                if (total == 3) {
                    core.ellipse(x * Tile.WIDTH + Tile.WIDTH * 0.5f, y * Tile.WIDTH + Tile.WIDTH * 0.75f, itemSize, itemSize);
                } else {
                    core.ellipse(x * Tile.WIDTH + Tile.WIDTH * 0.25f, y * Tile.WIDTH + Tile.WIDTH * 0.75f, itemSize, itemSize);
                }
                break;
            case 3:
                core.ellipse(x * Tile.WIDTH + Tile.WIDTH * 0.75f, y * Tile.WIDTH + Tile.WIDTH * 0.75f, itemSize, itemSize);
                break;
            default:
                PApplet.println("ERROR, itemSize for Tile is <" + total + "> instead of max 4!");
        }
    }

    public String getName() {
        return name;
    }

}