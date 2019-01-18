package de.tfour.main.java;

import processing.core.PApplet;

public class Item {
    private final Core core;

    private final int x;
    private final int y;
    private final char model;
    private final String name = "Potion";
    private final int color;

    public static Item create(Core core, int x, int y, char model) {
        Item e = new Item(core, x, y, model);
        Tile tile = core.getGame().getMap().getTile(x, y);
        if (tile != null && tile.hasItemSpace()) {
            tile.addItem(e);
            return e;
        }
        return null;
    }

    private Item(Core core, int x, int y, char model) {
        this.core = core;
        this.x = x;
        this.y = y;
        this.model = model;
        this.color = core.color(255);
    }

    public void drawAt(int tileX, int tileY) {
        core.fill(color);
        core.ellipse(tileX * Tile.WIDTH + Tile.WIDTH * 0.5f, tileY * Tile.WIDTH + Tile.WIDTH * 0.5f, Tile.WIDTH * 0.5f, Tile.WIDTH * 0.5f);
    }

    public void draw(int pos, int total) {
        core.fill(color);
        float xScale = 0.5f;
        float yScale = 0.5f;
        float itemSize = Tile.WIDTH * 0.35f;
        if (total != 1) {
            if (pos == 0) {
                xScale = 0.25f;
                if (total != 2) {
                    yScale = 0.25f;
                }
            } else if (pos == 1) {
                xScale = 0.75f;
                if (total != 2) {
                    yScale = 0.25f;
                }
            } else if (pos == 2) {
                yScale = 0.75f;
                if (total != 3) {
                    xScale = 0.25f;
                }
            } else if (pos == 3) {
                xScale = 0.75f;
                yScale = 0.75f;
            } else {
                PApplet.println("ERROR, itemSize for Tile is <" + total + "> instead of max 4!");
            }
        }
        core.ellipse(x * Tile.WIDTH + Tile.WIDTH * xScale, y * Tile.WIDTH + Tile.WIDTH * yScale, itemSize, itemSize);
    }

    public String getName() {
        return name;
    }

}