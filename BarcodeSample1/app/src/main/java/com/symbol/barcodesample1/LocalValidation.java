package com.symbol.barcodesample1;

import java.util.Date;

public class LocalValidation {
    private final Date date;
    private final Employee employee;
    private final Building building;

    private SentState sentState;
    private int id;

    public LocalValidation(Employee employee, Building building) {
        this.date = new Date();
        this.sentState = SentState.NOT_SENT;

        this.employee = employee;
        this.building = building;
    }

    public LocalValidation(int id, Date date, Employee employee, Building building, SentState sentState) {
        this.id = id;
        this.date = date;
        this.employee = employee;
        this.building = building;
        this.sentState = sentState;
    }

    public Date getDate() {
        return date;
    }

    public Employee getEmployee() {
        return employee;
    }
    public String getEmployeeNumber() {
        return employee.getNumber();
    }
    public String getEmployeeName() {
        return employee.getName();
    }

    public Building getBuilding() {
        return building;
    }
    public String getBuildingName() {
        return building.getName();
    }
    public String getBuildingNumber() {
        return building.getNumber();
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
