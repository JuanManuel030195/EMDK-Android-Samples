package com.symbol.barcodesample1;

public class Asset {
    private final String number;
    private final String description;
    private final String buildingName;
    private final int buildingId;
    private final String employeeNumber;

    public Asset(
        String number,
        String description,
        String buildingName,
        int buildingId,
        String employeeNumber
    ) {
        this.number = number;
        this.description = description;
        this.buildingName = buildingName;
        this.buildingId = buildingId;
        this.employeeNumber = employeeNumber;
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
    public String getEmployeeNumber() {
        return employeeNumber;
    }
}
