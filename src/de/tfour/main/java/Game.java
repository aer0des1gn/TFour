package de.tfour.main.java;

import processing.core.PApplet;
import processing.core.PConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Game {

    private final Core core;
    private Player player;

    //speech info
    private final ArrayList<String> bubbleText = new ArrayList<>();
    private final int bubbleDelay = 3;
    private int speechSecondsRemaining = 0;
    private int lastSecond = -1;

    private boolean manualMovementAllowed = true;
    private Creature toMove;
    private ArrayList<Creature> turnList;

    private final int backgroundColor;
    public static float theta = 0.0f;
    private GameModes mode;

    //build mode vars
    private Tile buildModeTile;
    private final GameMap map;

    private String consoleText = "";
    private boolean consoleOn;

    private boolean inventoryOpen;
    public static boolean ignoreFog = true;
    public static boolean ignoreVisibility = true;

    public enum GameModes {
        PLAY, DEBUG, PLACE, BUILD
    }

    public Game(Core core) {
        this.core = core;
        this.map = new GameMap(core);
        backgroundColor = core.color(0);
    }

    public void setup() {
        this.mode = GameModes.PLAY;
        this.buildModeTile = new Tile(core, -1, -1, 0);

        map.loadMap("map");

        Item.create(core, 3, 3, 'i');
        player = Player.create(core, 5, 5);
        turnList = new ArrayList<>();
        turnList.add(player);
        turnList.add(Creature.create(core, 2, 2, 'B'));
        turnList.add(Creature.create(core, 2, 14, 'B'));
        turnList.add(Creature.create(core, 11, 2, 'B'));
        nextTurn();
        map.updateTileVisibility();
        map.updateSeenTilesPercentage();
    }

    public void draw() {
        //update
        theta += 0.1f;
        core.translate(0, 1.5f * Tile.WIDTH);
        core.getSurface().setTitle("TestGame, FPS: " + Math.round(core.frameRate));
        if (mode == GameModes.BUILD) {
            drawBuildMode();
            return;
        }
        turnLogic();
        //draw
        drawBackground();
        if (inventoryOpen) {
            drawInventory();
        } else {
            map.drawTiles();
            map.updateTileVisibility();
            map.drawItemsAndCreatures();
            drawOverlay();
        }
        drawInfo();
    }

    private void turnLogic() {
        if (toMove.getAp() == 0) {
            nextTurn();
        }
        if (!(toMove instanceof Player) && (toMove.getNextMoves() == null || toMove.getNextMoves().isEmpty())) {
            toMove.randomTurn();
        }
        manualMovementAllowed = player.isMyTurn();
    }

    private void drawInventory() {
        for (Item i : player.getInventory().getItems()) {

        }
    }

    private void drawBuildMode() {
        drawBackground();
        map.drawTiles();
        core.noStroke();
        buildModeTile.drawAt((int) (Tile.WIDTH / 2), (int) (-1.5f * Tile.WIDTH), 1);
        core.stroke(1);
        buildModeTile.drawAt(core.mouseX, (int) (core.mouseY - 1.5f * Tile.WIDTH), 0.75f);
        core.textAlign(PConstants.LEFT, PConstants.CENTER);
        core.text("Current Tile: " + buildModeTile.getName(), Tile.WIDTH * 2, -1.5f * Tile.WIDTH + Tile.WIDTH / 2);
    }

    private void drawBackground() {
        switch (mode) {
            case DEBUG:
                core.background(127);
                break;
            case PLACE:
                core.background(core.color(0, 255, 0));
                break;
            case BUILD:
                core.background(core.color(255));
                break;
            case PLAY:
                core.background(backgroundColor);
                break;
        }
    }

    private void drawOverlay() {
        //fog
        Arrays.stream(map.getTiles()).flatMap(Arrays::stream).filter(tile -> !tile.isVisible()).forEachOrdered(Tile::drawFog);
        //handle speechBubble drawing
        if (speechSecondsRemaining == 0 && !bubbleText.isEmpty()) {
            bubbleText.remove(0);
            speechSecondsRemaining = bubbleDelay;
        }
        int bubbleNum = 0;
        for (String text : bubbleText) {
            drawTextBubble(text, bubbleNum);
            bubbleNum++;
        }
        //draw player movepool
        if (player.isMyTurn()) {
            for (Tile t : player.getMovepool()) {
                if (t.isVisible()) {
                    //core.fill(255, 50);
                    core.noFill();
                    core.stroke(255, 50);
                    core.ellipse(t.getX() * Tile.WIDTH + Tile.WIDTH / 2, t.getY() * Tile.WIDTH + Tile.WIDTH / 2, Tile.WIDTH * 0.6f, Tile.WIDTH * 0.6f);
                    //core.rect(t.getX() * Tile.WIDTH, t.getY() * Tile.WIDTH, Tile.WIDTH, Tile.WIDTH);
                }
            }
        }
        //draw enemies in reach
        for (Tile t : player.getTile().getNeighboursWithCreatures()) {
            core.noFill();
            core.stroke(255, 0, 0);
            core.ellipse(t.getX() * Tile.WIDTH + Tile.WIDTH / 2, t.getY() * Tile.WIDTH + Tile.WIDTH / 2, Tile.WIDTH * 0.9f, Tile.WIDTH * 0.9f);

        }
        //draws player path
        if (mode == GameModes.DEBUG && player.getNextMoves() != null) {
            for (Tile t : player.getNextMoves()) {
                core.fill(255, 0, 0);
                core.rect(t.getX() * Tile.WIDTH, t.getY() * Tile.WIDTH, Tile.WIDTH, Tile.WIDTH);
            }
        }

        //console
        if (consoleOn) {
            core.textAlign(PConstants.LEFT, PConstants.CENTER);
            core.fill(255);
            core.text(consoleText, 10, core.height / 2);
        }
    }

    private void drawInfo() {
        core.fill(255);
        //white hp bar
        core.rect(Tile.WIDTH * 8, -Tile.WIDTH, Tile.WIDTH * 4, Tile.WIDTH / 4);
        core.textAlign(PConstants.LEFT, PConstants.CENTER);
        core.text("AP: " + player.getAp(), Tile.WIDTH / 2, -1.5f * Tile.WIDTH + Tile.WIDTH / 2);
        core.text("Map: " + map.getSeenTilesPercentage() + "%", Tile.WIDTH * 12.5f, -1.5f * Tile.WIDTH + Tile.WIDTH / 2);
        core.textAlign(PConstants.RIGHT, PConstants.CENTER);
        core.text("HP: 000/000", Tile.WIDTH * 7.5f, -Tile.WIDTH);
        String turnbutton = player.myTurn ? "End Turn" : "AI Turn";
        core.text(turnbutton, 19 * Tile.WIDTH + Tile.WIDTH / 2, -1.5f * Tile.WIDTH + Tile.WIDTH / 2);
        if (core.mouseX > 16 * Tile.WIDTH && core.mouseY < Tile.WIDTH && toMove.equals(player)) {
            core.fill(255, 255, 120);
            core.text(turnbutton, 19 * Tile.WIDTH + Tile.WIDTH / 2 + 1, -1.5f * Tile.WIDTH + Tile.WIDTH / 2 + 1);
        }
        //green hp bar
        core.fill(0, 255, 0);
        core.rect(Tile.WIDTH * 8, -Tile.WIDTH, PApplet.map(player.getHp(), 0, player.getHpMax(), 0, Tile.WIDTH * 4), Tile.WIDTH / 4);
    }

    private void drawTextBubble(String text, int bubbleNumber) {
        @SuppressWarnings("SuspiciousNameCombination") float yOffset = Tile.WIDTH;
        float yOffComputed = yOffset * bubbleNumber;
        float y = 15.5f;
        core.fill(255);
        core.triangle(
                Tile.WIDTH / 3, y * Tile.WIDTH - yOffComputed + Tile.WIDTH / 4,
                Tile.WIDTH * 2 / 3, y * Tile.WIDTH - yOffComputed + Tile.WIDTH / 4,
                Tile.WIDTH / 2, y * Tile.WIDTH + Tile.WIDTH / 2 - yOffComputed);
        core.rect(0, (y - 0.75f) * Tile.WIDTH - yOffComputed + Tile.WIDTH / 4, core.textWidth(text), Tile.WIDTH * 3 / 4);
        core.fill(0);
        core.textAlign(PConstants.LEFT);
        core.text(text, 0f, y * Tile.WIDTH - Tile.WIDTH * 1 / 8 - yOffComputed + Tile.WIDTH / 4);
        if (PApplet.second() != lastSecond) {
            speechSecondsRemaining--;
            lastSecond = PApplet.second();
        }
        core.stroke(255, 0, 0);
        core.noStroke();
    }

    private void sendTextBubble(String text, int seconds) {
        bubbleText.add(text);
        speechSecondsRemaining = seconds;
    }

    public void sendTextBubble(String text) {
        sendTextBubble(text, bubbleDelay);
    }

    private int counter = 0;

    public void handleInput() {
        //handle mouse press
        if (core.mousePressed) {
            handleMouseInput();
        }
        //handle keyboard input
        if (core.keyPressed) {
            if (core.key == '<') {
                consoleOn = !consoleOn;
            }
            if (consoleOn) {
                if (core.keyCode == PConstants.BACKSPACE) {
                    consoleText = consoleText.substring(0, consoleText.length() - 1);
                } else {
                    if (core.key != '<') {
                        consoleText += core.key;
                    }
                }
            }
            if (core.key == '1') mode = (mode != GameModes.DEBUG) ? GameModes.DEBUG : GameModes.PLAY;
            if (core.key == '2') mode = (mode != GameModes.PLACE) ? GameModes.PLACE : GameModes.PLAY;
            if (core.key == '3') mode = (mode != GameModes.BUILD) ? GameModes.BUILD : GameModes.PLAY;
            if (manualMovementAllowed) {
                if (core.key == 'W' || core.key == 'w') player.move(0);
                if (core.key == 'A' || core.key == 'a') player.move(1);
                if (core.key == 'S' || core.key == 's') player.move(2);
                if (core.key == 'D' || core.key == 'd') player.move(3);
            }
            if ((core.key == 'E' || core.key == 'e') && player.isMyTurn()) nextTurn();
            if (core.key == 'N' || core.key == 'n') nextTurn();
            if (core.key == 'S' || core.key == 's') map.saveMap();
        }

    }

    private void handleMouseInput() {
        int posX = (int) (core.mouseX / Tile.WIDTH);
        int posY = (int) (core.mouseY / Tile.WIDTH - 1.5f);
        Tile clickedTile = map.getTile(posX, posY);

        switch (mode) {
            case BUILD:
                switch (core.mouseButton) {
                    case PConstants.LEFT:
                        map.setTile(posX, posY, new Tile(core, posX, posY, buildModeTile.getId()));
                        break;
                    case PConstants.RIGHT:
                        int id = buildModeTile.getId();
                        id++;
                        if (id > Tile.HIGHEST_ID) id = 0;
                        buildModeTile = new Tile(core, -1, -1, id);
                        break;
                    case PConstants.CENTER:
                        buildModeTile = new Tile(core, -1, -1, clickedTile.getId());
                        break;
                }
                break;
            case PLACE:
                switch (core.mouseButton) {
                    case PConstants.LEFT:
                        Creature.create(core, posX, posY, 'x');
                        break;
                    case PConstants.RIGHT:
                        Item.create(core, posX, posY, 'i');
                        break;
                    case PConstants.CENTER:
                        //currently unused
                        break;
                }
                break;
            case DEBUG:
                if (core.mouseButton == PConstants.CENTER) {
                    sendTextBubble("test" + counter);
                    counter++;
                }
            case PLAY:
                if (player.isMyTurn()) {
                    if (player.getTile().getNeighboursWithCreatures().contains(clickedTile)) {
                        player.attack(clickedTile.getCreature());
                    } else if (player.getMovepool().contains(clickedTile)) {
                        player.setNextMoves(map.astar(player.getTile(), clickedTile));
                    } else if ((core.mouseX > (16 * Tile.WIDTH)) && (core.mouseY < Tile.WIDTH) && (core.mouseButton == PConstants.LEFT)) {
                        nextTurn();
                    } else if (clickedTile!=null) {
                        sendTextBubble("Not enough AP!");
                    }
                }
                break;
        }
    }

    public void nextTurn() {
        if (toMove != null)
            toMove.endTurn();
        toMove = turnList.get(0);
        toMove.startTurn();
        Collections.rotate(turnList, 1);
    }

    public GameModes getMode() {
        return mode;
    }

    public void setManualMovementAllowed(boolean manualMovementAllowed) {
        this.manualMovementAllowed = manualMovementAllowed;
    }

    public void setToMove(Creature toMove) {
        this.toMove = toMove;
    }

    public Creature getToMove() {
        return toMove;
    }

    public ArrayList<Creature> getTurnList() {
        return turnList;
    }

    public Player getPlayer() {
        return player;
    }

    public GameMap getMap() {
        return map;
    }
}