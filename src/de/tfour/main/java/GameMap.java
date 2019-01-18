package de.tfour.main.java;

import processing.core.PApplet;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

class GameMap {

    private final Core core;
    private final Tile[][] tiles;
    private String name = "devmap";
    //how many tiles have been seen on the map
    private int seenTilesPercentage;

    public GameMap(Core core) {
        this.core = core;
        tiles = new Tile[Core.widthInTiles][Core.heightInTiles];
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

    public ArrayList<Tile> astar(Tile start, Tile goal) {
        int[][] gweight = new int[tiles.length][tiles[0].length];
        ArrayList<Tile> openList = new ArrayList<>();
        HashMap<Tile, Tile> predecessors = new HashMap<>();
        HashSet<Tile> closedList = new HashSet<>();
        openList.add(start);
        while (!openList.isEmpty()) {
            Tile min = openList.get(0);
            for (Tile t : openList) {
                if (gweight[t.getX()][t.getY()] < gweight[min.getX()][min.getY()]) {
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

    public void saveMap() {
        try {
            FileWriter writer = new FileWriter("src/de/tfour/main/resources/" + name + ".txt");
            PrintWriter print_line = new PrintWriter(writer);
            StringBuilder toWrite = new StringBuilder();
            for (int y = 0; y < tiles[0].length; y++) {
                for (Tile[] tile : tiles) {
                    toWrite.append(tile[y].getId()).append(" ");
                }
                toWrite = new StringBuilder(toWrite.substring(0, toWrite.length() - 1));
                toWrite.append("\n");
            }
            toWrite = new StringBuilder(toWrite.substring(0, toWrite.length() - 1));
            print_line.print(toWrite);
            print_line.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadMap(String mapName) {
        this.name = mapName;
        File file = new File("src/de/tfour/main/resources/" + mapName + ".txt");
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
    }

    public void updateTileVisibility() {
        for (Tile t : core.getGame().getPlayer().getTile().getTilesInRadius(4)) {
            t.setSeen(true);
            t.setVisible(true);
        }
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
        seenTilesPercentage = (int) PApplet.map(noSeenTiles, 0, noTiles, 0, 100);
    }

    public void setTile(int x, int y, Tile t) {
        tiles[x][y] = t;
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= Core.widthInTiles || y >= Core.heightInTiles) return null;
        return tiles[x][y];
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public int getSeenTilesPercentage() {
        return seenTilesPercentage;
    }
}
