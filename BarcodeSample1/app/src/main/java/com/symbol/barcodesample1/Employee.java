package com.symbol.barcodesample1;

public class Employee {
    private final String number;
    private final String name;
    private final int level;

    public Employee(String number, String name, int level) {
        this.number = number;
        this.name = name;
        this.level = level;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }
}
