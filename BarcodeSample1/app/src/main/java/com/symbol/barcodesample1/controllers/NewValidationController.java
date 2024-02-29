package com.symbol.barcodesample1.controllers;

import android.app.Activity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.symbol.barcodesample1.Building;
import com.symbol.barcodesample1.Employee;
import com.symbol.barcodesample1.R;

import java.util.ArrayList;

public class NewValidationController {
    private final Activity activity;
    private final View view;
    private Employee[] employees = new Employee[0];
    private Building[] buildings = new Building[0];

    public NewValidationController(Activity activity, View view) {
        this.activity = activity;
        this.view = view;
    }

    public void setEmployees(Employee[] employees) {
        this.employees = employees;
    }

    public void setBuildings(Building[] buildings) {
        this.buildings = buildings;
    }

    public void setEmployeesToSpinner() {
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) view.findViewById(R.id.employeeSpinner);

        ArrayList<String> employeeNames = new ArrayList<String>();
        for (Employee employee : this.employees) {
            employeeNames.add(employee.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activity,
                android.R.layout.simple_dropdown_item_1line,
                employeeNames
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoCompleteTextView.setAdapter(adapter);
    }

    private void setBuildingsToSpinner() {
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) view.findViewById(R.id.buildingSpinner);

        ArrayList<String> buildingNames = new ArrayList<String>();
        for (Building building : this.buildings) {
            buildingNames.add(building.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activity,
                android.R.layout.simple_dropdown_item_1line,
                buildingNames
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoCompleteTextView.setAdapter(adapter);
    }
}
