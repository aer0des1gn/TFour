package de.tfour.main.java;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.InputMismatchException;

public class Creature {

    protected Core core;

    //counts how many times the Creature constructor has been called
    public static int creaturecount = 0;

    //name of the creature
    protected String name;

    //hitpoints (currently)
    protected int hp;
    //hitpoint maximum
    protected int hpMax;
    //range in which the creature can detect other creatures
    protected int awareness;

    //tile position (e.g. 5|5)
    protected int x, y;
    //current pixel position (e.g. a tile position of 5|5 means a pixel position of 5*Tile.WIDTH|5*Tile.WIDTH)
    protected int pixelX, pixelY;

    //how many pixels a creature can move per frame
    protected final int movespeed = 3;
    //counts how many pixels the creature moved since starting movement (necessary for the animation process)
    protected int moveCount = 0;
    //true if creature is moving, false if not
    protected boolean moving;
    protected int direction;

    protected PImage img[];
    protected int animationOffset = 1;
    protected boolean animationForwards = true;
    protected char model;

    protected Inventory inventory;

    protected HashSet<Tile> movepool;
    protected int actionsPerTurn;
    protected int actionsRemaining;
    protected ArrayList<Tile> nextMoves;

    protected boolean myTurn = false;

    public static Creature create(Core core, int x, int y, char model) {
        Creature c = new Creature(core, x, y, model);
        Tile tile = core.getGame().getTile(x, y);
        if (tile != null && tile.getCreature() == null) {
            tile.setCreature(c);
        }
        return c;
    }

    protected Creature(Core core, int x, int y, char model) {
        this.core = core;
        creaturecount++;
        this.x = x;
        this.y = y;
        this.model = model;
        this.inventory = new Inventory(core);
        this.moving = false;
        this.name = "Creature" + creaturecount;
        this.hpMax = 20;
        this.hp = hpMax;
        this.pixelX = (int) (x * Tile.WIDTH);
        this.pixelY = (int) (y * Tile.WIDTH);
        this.actionsPerTurn = 6;
        this.actionsRemaining = actionsPerTurn;
        this.movepool = calcMovePool();
        this.direction = 2;
        this.img = new PImage[12];
        int randint = 1 + core.floor(core.random(3));
        for (int i = 0; i < 12; i++) {
            img[i] = core.loadImage("src/de/tfour/main/resources/enemy/e" + randint + core.nf(i, 2) + ".png");
        }
    }

    public void draw() {
        core.fill(0);
        core.textAlign(PConstants.CENTER, PConstants.CENTER);
        if (moving) {
            switch (direction) {
                default:
                    break;
                case 0:
                    pixelY -= movespeed;
                    break;
                case 1:
                    pixelX -= movespeed;
                    break;
                case 2:
                    pixelY += movespeed;
                    break;
                case 3:
                    pixelX += movespeed;
                    break;
            }
        }
        moveCount += movespeed;
        if (moveCount >= Tile.WIDTH) {
            setMoving(false);
            moveCount = 0;
            calculatePixelPositions();
        }
        if (!getTile().isSeen()) return;
        if (img == null) {
            core.text(model, pixelX + Tile.WIDTH / 2, pixelY + Tile.WIDTH / 2);
            return;
        } else {
            int animationIndex = direction + animationOffset * 4;
            if (myTurn) {
                if (core.frameCount % 30 == 0) {
                    if (animationForwards) {
                        animationOffset++;
                    } else animationOffset--;
                    if (animationOffset == 0 || animationOffset == 2) animationForwards = !animationForwards;
                }
            }
            core.image(img[animationIndex], pixelX, pixelY, Tile.WIDTH, Tile.WIDTH);
        }

        core.fill(255);
        core.rect(pixelX, pixelY, Tile.WIDTH, 10);
        core.fill(0, 255, 0);
        core.rect(pixelX, pixelY, core.map(hp, 0, hpMax, 0, Tile.WIDTH), 10);
    }

    public boolean move(Tile t) {
        Tile myTile = getTile();
        if (!myTile.getViableNeighbours().contains(t)) {
            PApplet.println("Error, cannot move to a non-adjacent tile!");
            //PApplet.println("Error, not a neighbouring tile!\n" + t.toString());
            return false;
        }
        if (myTile.getY() - t.getY() < 0) move(2);
        if (myTile.getX() - t.getX() < 0) move(3);
        if (myTile.getY() - t.getY() > 0) move(0);
        if (myTile.getX() - t.getX() > 0) move(1);
        return true;
    }

    public boolean move(int direction) {
        //WASD -> 0123
        if (direction > 3 || direction < 0) {
            throw new InputMismatchException();
        }
        int targetX = -1;
        int targetY = -1;
        switch (direction) {
            case 0:
                targetX = x;
                targetY = y - 1;
                break;
            case 1:
                targetX = x - 1;
                targetY = y;
                break;
            case 2:
                targetX = x;
                targetY = y + 1;
                break;
            case 3:
                targetX = x + 1;
                targetY = y;
                break;
        }
        this.direction = direction;
        if (targetX == -1 || targetY == -1) return false;
        Tile targetTile = core.getGame().getTile(targetX, targetY);
        if (targetTile == null || targetTile.isSolid() || targetTile.getCreature() != null || !movepool.contains(targetTile))
            return false;

        //movement successful
        targetTile.setCreature(this);
        core.getGame().getTile(x, y).setCreature(null);
        x = targetX;
        y = targetY;
        ArrayList<Item> items = targetTile.getItems();
        setMoving(true);
        actionsRemaining--;
        movepool = calcMovePool();

        //handle item pickup (if any)
        if (items.isEmpty()) return true;
        for (Item i : items) {
            inventory.addItem(i);
            core.getGame().sendTextBubble("+" + i.getName());
        }
        items.clear();
        return true;
    }

    private void calculatePixelPositions() {
        pixelX = (int) (x * Tile.WIDTH);
        pixelY = (int) (y * Tile.WIDTH);
    }

    public void setMoving(boolean isMoving) {
        this.moving = isMoving;
        if (!moving) {
            core.getGame().setManualMovementAllowed(true);
        } else {
            core.getGame().setManualMovementAllowed(false);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Tile getTile() {
        return core.getGame().getTile(x, y);
    }

    public HashSet<Tile> calcMovePool() {
        Tile currentTile = getTile();
        HashSet<Tile> movepool = new HashSet<>();
        if (actionsRemaining <= 0) {
            if (actionsRemaining < 0)
                PApplet.println("Error! Negative actions left for " + name);
            return movepool;
        }
        movepool.addAll(currentTile.getViableNeighbours());
        for (int i = 0; i < actionsRemaining - 1; i++) {
            ArrayList<Tile> toAdd = new ArrayList<>();
            for (Tile t : movepool) {
                toAdd.addAll(t.getViableNeighbours());
            }
            movepool.addAll(toAdd);
        }
        movepool.remove(getTile());
        return movepool;
    }

    public HashSet<Tile> getMovepool() {
        return movepool;
    }

    public int getActionsRemaining() {
        return actionsRemaining;
    }

    public void startTurn() {
        actionsRemaining = actionsPerTurn;
        core.getGame().setToMove(this);
        movepool = calcMovePool();
        myTurn = true;
        Tile tile = getTile();
        TileEvent event = tile.getTileEvent();
        if (event != null) event.execute(tile);
    }

    public void endTurn() {
        animationOffset = 1;
        myTurn = false;
    }

    public void randomTurn() {
        actRandomly(actionsRemaining);
    }

    public void actRandomly(int howoften) {
        //random movement
        if (nextMoves == null) nextMoves = new ArrayList<>();
        Tile tile = getTile();
        for (int i = 0; i < howoften; i++) {
            Tile randomNeighbour = tile.getRandomViableNeighbour();
            if (randomNeighbour == null) {
                core.getGame().nextTurn();
                return;
            }
            nextMoves.add(randomNeighbour);
            tile = randomNeighbour;
        }
    }

    public void setNextMoves(ArrayList<Tile> nextMoves) {
        this.nextMoves = nextMoves;
    }

    public ArrayList<Tile> getNextMoves() {
        return nextMoves;
    }

    public boolean isMoving() {
        return moving;
    }

    public int getPixelX() {
        return pixelX;
    }

    public int getPixelY() {
        return pixelY;
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public int getHpMax() {
        return hpMax;
    }

    //returns whether a killing blow was dealt
    public boolean damage(int dmg) {
        hp -= dmg;
        if (hp <= 0) {
            hp = 0;
            die();
            return true;
        }
        return false;
    }

    public void die() {
        if (core.getGame().getTurnList().contains(this)) {
            core.getGame().getTurnList().remove(this);
        }
        actionsRemaining = 0;
        getTile().setCreature(null);
    }

    public void attack(Creature c) {
        if (c.damage(5)) {
            //some reward
        }
        actionsRemaining--;
        movepool = calcMovePool();
    }
}