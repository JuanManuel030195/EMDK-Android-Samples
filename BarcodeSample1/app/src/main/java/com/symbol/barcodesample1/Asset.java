package com.symbol.barcodesample1;

public class Asset {
    private final String number;
    private final String description;
    private final String buildingName;
    private final int buildingId;

    public Asset(String number, String description, String buildingName, int buildingId) {
        this.number = number;
        this.description = description;
        this.buildingName = buildingName;
        this.buildingId = buildingId;
    }

    public String getNumber() {
        return number;
    }

    public String getDescription() {
        return description;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public int getBuildingId() {
        return buildingId;
    }
}
