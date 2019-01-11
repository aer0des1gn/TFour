package de.tfour.main.java;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.ArrayList;
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
    //direction in which the creature faces right now. 0 1 2 3 -> up left down right
    protected int direction;

    //the sprite sheet of the character
    protected PImage img[];
    //determines which sprite of the spritesheet should currently be drawn
    protected int animationOffset = 1;
    //whether the animation of the sprites is running forwards or backwards
    protected boolean animationForwards = true;
    //a single char representing the creature. if there's no spritesheet (yet), this is drawn instead.
    protected char model;

    //the creature's inventory
    protected Inventory inventory;

    //the pool of tiles which the creature can move to this turn, depending on ap currently remaining
    protected HashSet<Tile> movepool;
    //how many ap you currently have. ap stands for action points. most actions and movement cost ap.
    protected int ap;
    //how many ap you start your turn with
    protected int apPerTurn;
    //movement buffer for the moves the creature will take in its next turns. this is where movement from pathfinding is stored.
    protected ArrayList<Tile> nextMoves;

    //is true if it's the creature's turn, otherwise false
    protected boolean myTurn = false;

    //method that constructs a creature and then assigns it to a tile.
    public static Creature create(Core core, int x, int y, char model) {
        Creature c = new Creature(core, x, y, model);
        Tile tile = core.getGame().getTile(x, y);
        if (tile != null && tile.getCreature() == null) {
            tile.setCreature(c);
        }
        return c;
    }

    //constructor. direct use of the constructor is not recommended, because tile is not assigned. use create() instead.
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
        this.apPerTurn = 6;
        this.ap = apPerTurn;
        this.movepool = calcMovePool();
        this.direction = 2;
        this.img = new PImage[12];
        int randint = 1 + core.floor(core.random(3));
        for (int i = 0; i < 12; i++) {
            img[i] = core.loadImage("src/de/tfour/main/resources/enemy/e" + randint + core.nf(i, 2) + ".png");
        }
    }

    //update and draw the creature
    public void draw() {
        //update pixel coordinates
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
        //if moveCount is bigger than Tile.WIDTH the creature moved a whole tile, so movement should end.
        if (moveCount >= Tile.WIDTH) {
            setMoving(false);
            moveCount = 0;
            //pixelpos are calculated again in case the creature "overshot" the goal by a few pixels.
            calculatePixelPositions();
        }
        //if the tile the creature is on has not yet been discovered, it's not drawn. as easy as that!
        if (!getTile().isSeen()) return;
        //draw the creature
        if (img == null) {
            //if there's no spritesheet, draw the model char instead
            core.fill(0);
            core.textAlign(PConstants.CENTER, PConstants.CENTER);
            core.text(model, pixelX + Tile.WIDTH / 2, pixelY + Tile.WIDTH / 2);
            return;
        } else {
            //draw the sprite and calculate the right animationOffset based on the current frame
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

        //draw the health bar of the creature
        core.fill(255);
        core.rect(pixelX, pixelY, Tile.WIDTH, 10);
        core.fill(0, 255, 0);
        core.rect(pixelX, pixelY, core.map(hp, 0, hpMax, 0, Tile.WIDTH), 10);
    }

    //move to an adjacent tile
    public boolean move(Tile t) {
        Tile myTile = getTile();
        if (!myTile.getViableNeighbours().contains(t)) {
            PApplet.println("Error, cannot move to a non-adjacent tile!");
            return false;
        }
        if (myTile.getY() - t.getY() < 0) move(2);
        if (myTile.getX() - t.getX() < 0) move(3);
        if (myTile.getY() - t.getY() > 0) move(0);
        if (myTile.getX() - t.getX() > 0) move(1);
        return true;
    }

    //move to an adjacent tile, based on a direction
    public boolean move(int direction) {
        //remember, WASD -> 0123
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
        ap--;
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

    //this method is needed because the game disallows movement input while a creature is already moving.
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
        if (ap <= 0) {
            if (ap < 0)
                PApplet.println("Error! Negative actions left for " + name);
            return movepool;
        }
        movepool.addAll(currentTile.getViableNeighbours());
        for (int i = 0; i < ap - 1; i++) {
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

    public int getAp() {
        return ap;
    }

    public void startTurn() {
        ap = apPerTurn;
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
        actRandomly(ap);
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
        ap = 0;
        getTile().setCreature(null);
    }

    public void attack(Creature c) {
        if (c.damage(5)) {
            //some reward
        }
        ap--;
        movepool = calcMovePool();
    }
}