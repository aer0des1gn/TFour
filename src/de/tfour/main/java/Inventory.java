package de.tfour.main.java;

import java.util.ArrayList;

public class Inventory {

    private Core core;

    private ArrayList<Item> items;

    public Inventory(Core core) {
        this.core = core;
        this.items = new ArrayList<>();
    }

    public void addItem(Item i) {
        items.add(i);
    }

    public void removeItem(Item i) {
        items.remove(i);
    }

    public ArrayList<Item> getItems() {
        return items;
    }
}