package com.symbol.barcodesample1;

import java.util.Date;

public class LocalValidation {
    private final Date date;
    private final String employeeNumber;
    private final String building;

    private SentState sentState;
    private int id;

    public LocalValidation(String employeeNumber, String building) {
        this.date = new Date();
        this.sentState = SentState.NOT_SENT;

        this.employeeNumber = employeeNumber;
        this.building = building;
    }

    public Date getDate() {
        return date;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getBuilding() {
        return building;
    }

    public SentState getSentState() {
        return sentState;
    }

    public void setSentState(SentState sentState) {
        this.sentState = sentState;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
