package de.tfour.main.java;

import processing.core.PImage;

public class Player extends Creature {

    public static Player create(Core core, int x, int y) {
        Player p = new Player(core,x,y);
        Tile tile = core.getGame().getMap().getTile(x, y);
        if (tile != null && tile.getCreature() == null) {
            tile.setCreature(p);
        }
        return p;
    }

    private Player(Core core, int x, int y) {
        super(core, x, y, 'P');
        this.name = "Player";
        this.img = new PImage[12];
        for (int i = 0; i < 12; i++) {
            img[i] = core.loadImage("src/de/tfour/main/resources/player/p"+core.nf(i,2)+".png");
        }
    }

    public boolean move(int direction) {
        boolean out = super.move(direction);
        core.getGame().getMap().updateTileVisibility();
        core.getGame().getMap().updateSeenTilesPercentage();
        return out;
    }
}
