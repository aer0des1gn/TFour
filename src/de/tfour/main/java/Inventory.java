package de.tfour.main.java;

import java.util.ArrayList;

public class Inventory {

    private final Core core;

    private final ArrayList<Item> items;

    public static final int ITEMSTACK_MAX = 4;

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