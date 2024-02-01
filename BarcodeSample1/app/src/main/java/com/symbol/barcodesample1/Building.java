package com.symbol.barcodesample1;

public class Building {
    private final int id;
    private final String name;
    private final String number;

    public Building(int id, String name, String number) {
        this.id = id;
        this.name = name;
        this.number = number;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    @Override
    public String toString() {
    return name + " (" + number + "," + id + ")";
    }
}
