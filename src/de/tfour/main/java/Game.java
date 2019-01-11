package de.tfour.main.java;

import processing.core.PApplet;
import processing.core.PConstants;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Game {

    private Core core;
    private Tile[][] tiles;
    private Player player;

    //how many tiles have been seen on the map
    int seenTilesPercentage;

    //speech info
    private ArrayList<String> bubbleText = new ArrayList<>();
    private int bubbleDelay = 3;
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

    public enum GameModes {
        PLAY, DEBUG, PLACE, BUILD
    }

    public Game(Core core) {
        this.core = core;
        tiles = new Tile[Core.widthInTiles][Core.heightInTiles];
        backgroundColor = core.color(0);
    }

    public void setup() {
        this.mode = GameModes.PLAY;
        this.buildModeTile = new Tile(core, -1, -1, 0);

        loadMap();

        Item.create(core, 3, 3, 'i');
        player = Player.create(core, 5, 5);
        turnList = new ArrayList<>();
        turnList.add(player);
        turnList.add(Creature.create(core, 2, 2, 'B'));
        turnList.add(Creature.create(core, 2, 14, 'B'));
        turnList.add(Creature.create(core, 11, 2, 'B'));
        nextTurn();
        updateTileVisibility();
        updateSeenTilesPercentage();
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
        drawTiles();
        updateTileVisibility();
        drawItemsAndCreatures();
        drawOverlay();
        drawInfo();
    }

    public void updateTileVisibility() {
        for (Tile t : player.getTile().getTilesInRadius(4)) {
            t.setSeen(true);
            t.setVisible(true);
        }
    }

    public void turnLogic() {
        if (toMove.getAp() == 0) {
            nextTurn();
        }
        if (!(toMove instanceof Player) && (toMove.getNextMoves() == null || toMove.getNextMoves().isEmpty())) {
            toMove.randomTurn();
        }
        manualMovementAllowed = player.isMyTurn();
    }

    public ArrayList<Tile> astar(Tile start, Tile goal) {
        int[][] hweight = new int[core.getGame().tiles.length][core.getGame().tiles[0].length];
        int[][] gweight = new int[core.getGame().tiles.length][core.getGame().tiles[0].length];
        ArrayList<Tile> openList = new ArrayList();
        HashMap<Tile, Tile> predecessors = new HashMap<>();
        HashSet closedList = new HashSet();
        openList.add(start);
        while (!openList.isEmpty()) {
            Tile min = openList.get(0);
            for (Tile t : openList) {
                if (hweight[t.getX()][t.getY()] + gweight[t.getX()][t.getY()] < hweight[min.getX()][min.getY()] + gweight[min.getX()][min.getY()]) {
                    min = t;
                }
            }
            openList.remove(min);
            if (min.equals(goal)) {
                ArrayList<Tile> output = new ArrayList<>();
                Tile tile = goal;
                output.add(goal);
                while (predecessors.get(tile) != null) {
                    output.add(predecessors.get(tile));
                    tile = predecessors.get(tile);
                }
                output.remove(start);
                Collections.reverse(output);
                return output;
            }
            closedList.add(min);
            //expand
            for (Tile successor : min.getViableNeighbours()) {
                if (closedList.contains(successor))
                    continue;
                int tentative_g = gweight[min.getX()][min.getY()] + 1;
                if (openList.contains(successor) && tentative_g >= gweight[successor.getX()][successor.getY()])
                    continue;
                predecessors.put(successor, min);
                gweight[successor.getX()][successor.getY()] = tentative_g;
                if (!openList.contains(successor)) {
                    openList.add(successor);
                }
            }
        }
        return null;
    }

    public void drawBuildMode() {
        drawBackground();
        drawTiles();
        core.noStroke();
        buildModeTile.drawAt((int) (Tile.WIDTH / 2), (int) (-1.5f * Tile.WIDTH), 1);
        core.stroke(1);
        buildModeTile.drawAt(core.mouseX, (int) (core.mouseY - 1.5f * Tile.WIDTH), 0.75f);
        core.textAlign(PConstants.LEFT, PConstants.CENTER);
        core.text("Current Tile: " + buildModeTile.getName(), Tile.WIDTH * 2, -1.5f * Tile.WIDTH + Tile.WIDTH / 2);
    }

    public void drawBackground() {
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

    public void drawTiles() {
        core.noStroke();
        //draw tiles themselves first
        for (Tile[] tt : tiles) {
            for (Tile t : tt) {
                t.draw();
                t.setVisible(false);
            }
        }
    }

    public void drawItemsAndCreatures() {
        for (Tile[] tt : tiles) {
            for (Tile t : tt) {
                if (t.getCreature() != null) {
                    Creature c = t.getCreature();
                    if (c.getNextMoves() != null && !c.getNextMoves().isEmpty() && !c.isMoving()) {
                        c.move(c.getNextMoves().remove(0));
                    }
                }
                t.drawItems();
                t.drawCreature();
            }
        }
    }

    public void drawOverlay() {
        //fog
        for (Tile[] tt : tiles) {
            for (Tile t : tt) {
                if (!t.isVisible())
                    t.drawFog();
            }
        }
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
    }

    public void drawInfo() {
        core.fill(255);
        //white hp bar
        core.rect(Tile.WIDTH * 8, -Tile.WIDTH, Tile.WIDTH * 4, Tile.WIDTH / 4);
        core.textAlign(PConstants.LEFT, PConstants.CENTER);
        core.text("AP: " + player.getAp(), Tile.WIDTH / 2, -1.5f * Tile.WIDTH + Tile.WIDTH / 2);
        core.text("Map: " + seenTilesPercentage + "%", Tile.WIDTH * 12.5f, -1.5f * Tile.WIDTH + Tile.WIDTH / 2);
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
        core.rect(Tile.WIDTH * 8, -Tile.WIDTH, core.map(player.getHp(), 0, player.getHpMax(), 0, Tile.WIDTH * 4), Tile.WIDTH / 4);
    }

    public void drawTextBubble(String text, int bubbleNumber) {
        float yOffset = Tile.WIDTH;
        float yOffComputed = yOffset * bubbleNumber;
        float y = 15.5f;
        core.fill(255);
        core.triangle(Tile.WIDTH / 3, y * Tile.WIDTH - yOffComputed + Tile.WIDTH / 4,
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

    public void sendTextBubble(String text, int seconds) {
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
            int posX = (int) (core.mouseX / Tile.WIDTH);
            int posY = (int) (core.mouseY / Tile.WIDTH - 1.5f);
            Tile clickedTile = getTile(posX, posY);
            if (mode == GameModes.BUILD) {
                if (core.mouseButton == PConstants.LEFT) {
                    setTile(posX, posY, new Tile(core, posX, posY, buildModeTile.getId()));
                } else if (core.mouseButton == PConstants.RIGHT) {
                    int id = buildModeTile.getId();
                    id++;
                    if (id > Tile.HIGHEST_ID) id = 0;
                    buildModeTile = new Tile(core, -1, -1, id);
                } else if (core.mouseButton == PConstants.CENTER) {
                    buildModeTile = new Tile(core, -1, -1, clickedTile.getId());
                }
                return;
            }
            //end turn button
            if (core.mouseX > 16 * Tile.WIDTH && core.mouseY < Tile.WIDTH) {
                if (core.mouseButton == PConstants.LEFT && player.isMyTurn()) {
                    nextTurn();
                }
            }
            if (mode == GameModes.PLACE) {
                if (core.mouseButton == PConstants.LEFT) {
                    Creature.create(core, posX, posY, 'x');
                } else if (core.mouseButton == PConstants.RIGHT) {
                    Item.create(core, posX, posY, 'i');
                }
            } else if (mode == GameModes.DEBUG) {
                if (core.mouseButton == PConstants.CENTER) {
                    sendTextBubble("test" + counter);
                    counter++;
                }
            }
            if (player.isMyTurn()) {
                if (player.getTile().getNeighboursWithCreatures().contains(clickedTile)) {
                    player.attack(clickedTile.getCreature());
                } else if (player.getMovepool().contains(clickedTile))
                    player.setNextMoves(astar(player.getTile(), clickedTile));
            }
        }
        //handle keyboard input
        if (core.keyPressed) {
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
            if (core.key == 'S' || core.key == 's') saveMap("map2");
        }
    }

    public void saveMap(String name) {
        try {
            FileWriter writer = new FileWriter("src/de/tfour/main/resources/" + name + ".txt");
            PrintWriter print_line = new PrintWriter(writer);
            String toWrite = "";
            for (int y = 0; y < tiles[0].length; y++) {
                for (int x = 0; x < tiles.length; x++) {
                    toWrite += tiles[x][y].getId() + " ";
                }
                toWrite = toWrite.substring(0, toWrite.length() - 1);
                toWrite += "\n";
            }
            toWrite = toWrite.substring(0, toWrite.length() - 1);
            print_line.print(toWrite);
            print_line.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void nextTurn() {
        if (toMove != null)
            toMove.endTurn();
        toMove = turnList.get(0);
        toMove.startTurn();
        Collections.rotate(turnList, 1);
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= Core.widthInTiles || y >= Core.heightInTiles) return null;
        return tiles[x][y];
    }

    public void setTile(int x, int y, Tile t) {
        tiles[x][y] = t;
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

    public void loadMap() {
//        for (int i = 0; i < tiles.length; i++) {
//            for (int j = 0; j < tiles[0].length; j++) {
//                tiles[i][j] = new Tile(core, i, j, core.color(90, 225, 165), ' ', false);
//            }
//        }
        File file = new File("src/de/tfour/main/resources/map2.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;
            int y = 0;
            while ((st = br.readLine()) != null) {
                String[] chars = st.split(" ");
                int x = 0;
                for (String c : chars) {
                    tiles[x][y] = new Tile(core, x, y, Integer.parseInt(c));
                    x++;
                }
                y++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        astar(getTile(3, 3), getTile(10, 10));
    }

    public Creature getToMove() {
        return toMove;
    }

    public ArrayList<Creature> getTurnList() {
        return turnList;
    }

    public void updateSeenTilesPercentage() {
        int noTiles = 0;
        int noSeenTiles = 0;
        for (Tile[] tt : tiles) {
            for (Tile t : tt) {
                noTiles++;
                if (t.isSeen()) noSeenTiles++;
            }
        }
        seenTilesPercentage = (int) core.map(noSeenTiles, 0, noTiles, 0, 100);
    }
}