package de.tfour.main.java;

import processing.core.PConstants;

import java.util.ArrayList;
import java.util.Collections;

public class Tile {

    private Core core;

    public static final float WIDTH = 32;
    public static final int HIGHEST_ID = 6;

    private int x, y;
    private String name;
    private int colorBase;
    private int colorCurrent;

    private boolean visible;
    private boolean seen;

    private TileEvent tileEvent = null;

    private char model = ' ';
    private boolean solid;

    private ArrayList<Item> items;
    private Creature creature;
    private int id;

    private ANIMATION_TYPE animated;

    private enum ANIMATION_TYPE {
        NONE, SHINY, SPARKLY
    }

    public Tile(Core core, int x, int y, int id) {
        this.core = core;
        this.x = x;
        this.y = y;
        this.visible = false;
        this.seen = false;
        this.colorBase = 0;
        this.solid = false;
        this.animated = ANIMATION_TYPE.NONE;
        this.id = id;
        switch (id) {
            default:
            case 0: //grass
                this.name = "Grass";
                this.colorBase = core.color(core.unhex("34a870"));
                break;
            case 1: //stone
                this.name = "Stone";
                this.colorBase = core.color(core.unhex("6d7078"));
                this.solid = true;
                break;
            case 2: //water
                this.name = "Water";
                this.colorBase = core.color(core.unhex("1f50cc"));
                this.solid = true;
                this.animated = ANIMATION_TYPE.SPARKLY;
                break;
            case 3: //lava
                this.name = "Lava";
                this.colorBase = core.color(core.unhex("f52025"));
                this.solid = true;
                this.animated = ANIMATION_TYPE.SPARKLY;
                break;
            case 4: //water shallow
                this.name = "ShallowWater";
                this.colorBase = core.color(core.unhex("407cff"));
                this.animated = ANIMATION_TYPE.SPARKLY;
                break;
            case 5: //lava shallow
                this.name = "ShallowLava";
                this.colorBase = core.color(core.unhex("cc7a47"));
                this.animated = ANIMATION_TYPE.SPARKLY;
                this.tileEvent = new TileEvent() {
                    @Override
                    public void execute(Tile t) {
                        t.getCreature().damage(3);
                    }
                };
                break;
            case 6: //gold
                this.name = "Gold";
                this.colorBase = core.color(core.unhex("f2a53f"));
                this.solid = true;
                this.animated = ANIMATION_TYPE.SHINY;
                break;
            case 7: //carpet
                this.name = "Carpet";
                this.colorBase = core.color(core.unhex("a63a3a"));
        }
        this.items = new ArrayList<>();

        float r = core.red(colorBase);
        float g = core.green(colorBase);
        float b = core.blue(colorBase);

        float rand1 = core.random(-10, 10);
        float rand2 = core.random(-10, 10);
        float rand3 = core.random(-10, 10);

        colorBase = core.color(r + rand1, g + rand2, b + rand3);
        this.colorCurrent = colorBase;
    }

    public void drawAt(int x0, int y0, float percentage) {
        core.fill(colorBase);
        core.rect(x0, y0, WIDTH * percentage, WIDTH * percentage);
    }

    public void draw() {
        if (seen) {
            if (animated != ANIMATION_TYPE.NONE) {
                float amp = 15;
                float r = core.red(colorBase);
                float g = core.green(colorBase);
                float b = core.blue(colorBase);
                if (animated == ANIMATION_TYPE.SHINY) {
                    float rand1 = Core.sin(Game.theta) * amp;
                    colorCurrent = (core.color(r + rand1, g + rand1, b + rand1));
                } else if (animated == ANIMATION_TYPE.SPARKLY) {
                    if (core.frameCount % 30 == 0) {
                        float rand1 = core.random(-amp, amp);
                        float rand2 = core.random(-amp, amp);
                        float rand3 = core.random(-amp, amp);
                        colorCurrent = (core.color(r + rand1, g + rand2, b + rand3));
                    }
                }
                core.fill(colorCurrent);
            } else {
                core.fill(colorBase);
            }
            core.rect(x * WIDTH, y * WIDTH, WIDTH, WIDTH);
        }
        //debug
        if (core.getGame().getMode() == Game.GameModes.DEBUG) {
            core.textSize(Tile.WIDTH * 0.5f);
            core.fill(255);
            core.textAlign(PConstants.LEFT, PConstants.TOP);
            core.text(x, x * WIDTH, y * WIDTH);
            core.text(y, x * WIDTH, y * WIDTH + WIDTH * 0.5f);
        }
    }

    public void drawFog() {
        core.fill(0,100);
        core.rect(x * WIDTH, y * WIDTH, WIDTH, WIDTH);
    }

    public void drawCreature() {
        //model
        core.textSize(Tile.WIDTH * 0.75f);
        if (creature != null) {
            creature.draw();
        } else {
            core.fill(0);
            core.textAlign(PConstants.CENTER, PConstants.CENTER);
            core.text(model, x * WIDTH + WIDTH * 0.5f, y * WIDTH + WIDTH * 0.5f);
        }
    }

    public void drawItems() {
        int itempos = 0;
        for (Item i : items) {
            i.draw(itempos, items.size());
            itempos++;
        }
    }

    public String toString() {
        String creatureName = (creature == null) ? "{}" : creature.toString();
        String entitystring = "Entities = {";
        for (Item e : items) {
            if (e != null) {
                entitystring += e.toString() + "\n";
            }
        }
        entitystring += "}";
        return "Tile " + model + ":\n" +
                "x = " + x + "\n" +
                "y = " + y + "\n" +
                "colorBase = " + (int) core.red(colorBase) + "|"
                + (int) core.green(colorBase) + "|"
                + (int) core.blue(colorBase) + "|"
                + (int) core.alpha(colorBase) + "\n" +
                "Creature = " + creatureName + "\n" +
                entitystring + "\n";
    }

    public boolean hasItemSpace() {
        return (items.size() < 4);
    }

    public boolean addItem(Item e) {
        if (items.size() >= 4) return false;
        return items.add(e);
    }

    public static int getDistance(Tile a, Tile b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    public void red() {
        this.colorBase = core.color(255, 0, 0);
    }

    public ArrayList<Tile> getNeighbours() {
        ArrayList neighbours = new ArrayList();
        neighbours.add(core.getGame().getTile(x + 1, y));
        neighbours.add(core.getGame().getTile(x, y + 1));
        neighbours.add(core.getGame().getTile(x - 1, y));
        neighbours.add(core.getGame().getTile(x, y - 1));
        neighbours.removeAll(Collections.singleton(null));
        return neighbours;
    }

    public ArrayList<Tile> getNeighboursWithCreatures() {
        ArrayList<Tile> out = getNeighbours();
        out.removeIf(t -> (t.getCreature() == null));
        return out;
    }

    public ArrayList<Tile> getViableNeighbours() {
        ArrayList<Tile> neighbours = new ArrayList();
        neighbours.add(core.getGame().getTile(x + 1, y));
        neighbours.add(core.getGame().getTile(x, y + 1));
        neighbours.add(core.getGame().getTile(x - 1, y));
        neighbours.add(core.getGame().getTile(x, y - 1));
        neighbours.removeAll(Collections.singleton(null));
        neighbours.removeIf(t -> t.isSolid());
        neighbours.removeIf(t -> t.getCreature() != null);
        return neighbours;
    }

    public Tile getRandomViableNeighbour() {
        ArrayList<Tile> viables = getViableNeighbours();
        if (viables.size() == 0) return null;
        int randindex = core.floor(core.random(viables.size()));
        return viables.get(randindex);
    }

    public ArrayList<Tile> getTilesInRadius(int radius) {
        ArrayList<Tile> tiles = new ArrayList<>();
        for (int i = x - radius; i <= x + radius; i++) {
            for (int i2 = y - radius; i2 <= y + radius; i2++) {
                tiles.add(core.getGame().getTile(i, i2));
            }
        }
        tiles.removeAll(Collections.singleton(null));
        tiles.removeIf(t -> Tile.getDistance(t, this) > radius);
        return tiles;
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public Creature getCreature() {
        return creature;
    }

    public void setCreature(Creature creature) {
        this.creature = creature;
    }

    public boolean isSolid() {
        return solid;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TileEvent getTileEvent() {
        return tileEvent;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}
