package com.symbol.barcodesample1;

import org.json.JSONException;
import org.json.JSONObject;

public class Asset {
    private final String number;
    private final String description;
    private final String buildingName;
    private int buildingId;
    private String employeeNumber;

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

    public void setBuildingId(int id) {
        this.buildingId = id;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("number", number);
        json.put("description", description);
        json.put("buildingName", buildingName);
        json.put("buildingId", buildingId);
        json.put("employeeNumber", employeeNumber);
        return json;
    }
}
