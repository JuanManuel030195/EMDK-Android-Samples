package com.symbol.barcodesample1.controllers;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.symbol.barcodesample1.Asset;
import com.symbol.barcodesample1.Building;
import com.symbol.barcodesample1.Employee;
import com.symbol.barcodesample1.MainActivity;
import com.symbol.barcodesample1.R;
import com.symbol.barcodesample1.SentState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class FetchController {

    private void loadSpinnerData() {
        setAssets(dbHandler.getAssets());

        if (currentEmployee.getLevel() == 2) {
            setEmployees(dbHandler.getEmployees());
        } else {
            setEmployees(new Employee[] { currentEmployee });
        }
        setEmployeesToSpinner();

        Building[] buildings = dbHandler.getBuildings();
        if (currentEmployee.getLevel() != 2) {
            Asset[] assetsFromCurrentEmployee = new Asset[0];
            for (Asset asset: this.assets) {
                if (asset.getEmployeeNumber().equals(currentEmployee.getNumber())) {
                    assetsFromCurrentEmployee = new Asset[assetsFromCurrentEmployee.length + 1];
                    assetsFromCurrentEmployee[assetsFromCurrentEmployee.length - 1] = asset;
                }
            }

            Building[] buildingFromAssets = new Building[0];
            for (Asset asset: assetsFromCurrentEmployee) {
                boolean buildingIsInArray = false;
                for (Building building: buildingFromAssets) {
                    if (building.getName().equals(asset.getBuildingName())) {
                        buildingIsInArray = true;
                        break;
                    }
                }

                if (!buildingIsInArray) {
                    buildingFromAssets = new Building[buildingFromAssets.length + 1];
                    buildingFromAssets[buildingFromAssets.length - 1] = dbHandler.getBuildingById(asset.getBuildingId());
                }
            }

            buildings = buildingFromAssets;
        }
        setBuildings(buildings);
        setBuildingsToSpinner();

        updateSyncButtonsText();
    }

    private void updateSyncButtonsText() {
        String buttonText;

        long totalAssets = dbHandler.getTotalAssets();
        buttonText = getResources().getString(R.string.sync_with_server_button_text);
        buttonText = buttonText + " (" + totalAssets + " en local)";
        Button syncWithServerButton = (Button) findViewById(R.id.syncWithServerButton);
        syncWithServerButton.setText(buttonText);

        buttonText = getResources().getString(R.string.sync_employees);
        buttonText = buttonText + " (" + this.employees.length + " en local)";
        Button getEmployeesButton = (Button) findViewById(R.id.syncEmployeesWithServerButton);
        getEmployeesButton.setText(buttonText);

        buttonText = getResources().getString(R.string.sync_buildings);
        buttonText = buttonText + " (" + this.buildings.length + " en local)";
        Button getBuildingsButton = (Button) findViewById(R.id.syncBuildingsWithServerButton);
        getBuildingsButton.setText(buttonText);

        long totalValidations = dbHandler.getTotalValidations(SentState.NOT_SENT);
        buttonText = getResources().getString(R.string.confrontas_pendientes);
        buttonText = buttonText + " (" + totalValidations + " en local)";
    }

    private void onFetchError(IOException e) {
        TextView textViewLoginStatus = (TextView) findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(e.getMessage());
        textViewLoginStatus.setVisibility(View.VISIBLE);
    }

    private void onGetBuildingsSeccess(JSONObject requestBody) throws JSONException {
        JSONArray buildings = responseBody.getJSONArray("buildings");
        Building[] buildingsArray = new Building[buildings.length()];
        for (int i = 0; i < buildings.length(); i++) {
            JSONObject building = buildings.getJSONObject(i);

            String buildingNuber;
            if (
                    building.has("numero") &&
                            !building.isNull("numero")
            ) {
                buildingNuber = building.getString("numero").length() > 0
                        ? building.getString("numero")
                        : "";
            } else {
                buildingNuber = "";
            }

            buildingsArray[i] = new Building(
                    building.getInt("idEdificio"),
                    building.getString("nombre").length() > 0
                            ? building.getString("nombre")
                            : "Edificio sin nombre",
                    buildingNuber
            );
        }

        Toast.makeText(
                MainActivity.this,
                "Buildings: " + buildings.length(),
                Toast.LENGTH_LONG
        ).show();

        dbHandler.clearBuildings();
        dbHandler.addBuildings(buildingsArray);
        setBuildings(buildingsArray);
        setBuildingsToSpinner();

        updateSyncButtonsText();
    }

    private void onGetEmployeesSuccess(JSONObject responseBody) throws JSONException {
        extViewLoginStatus.setVisibility(View.GONE);

        JSONArray employees = responseBody.getJSONArray("employees");
        Employee[] employeesArray = new Employee[employees.length()];
        for (int i = 0; i < employees.length(); i++) {
            JSONObject employee = employees.getJSONObject(i);
            employeesArray[i] = new Employee(
                    employee.getString("numeroEmpleado"),
                    employee.getString("nombre"),
                    employee.getInt("nivel")
            );
        }

        Toast.makeText(
                MainActivity.this,
                "Employees: " + employees.length(),
                Toast.LENGTH_LONG
        ).show();

//                            dbHandler.clearEmployees();
        dbHandler.addEmployees(employeesArray);
        setEmployees(employeesArray);
        setEmployeesToSpinner();

        dbHandler.updateEmployeePassword(this.currentEmployee, this.password);

        updateSyncButtonsText();
    }

    private void onGetAssetsSuccess(JSONObject responseBody) throws JSONException {
        textViewLoginStatus.setVisibility(View.GONE);

        JSONArray assets = responseBody.getJSONArray("activos");
        Asset[] assetsArray = new Asset[assets.length()];
        for (int i = 0; i < assets.length(); i++) {

            JSONObject asset = assets.getJSONObject(i);
            assetsArray[i] = new Asset(
                    asset.getString("numeroSAP"),
                    asset.getString("descripcion"),
                    asset.getString("edificio"),
                    asset.isNull("idEdificio") ? 0 : asset.getInt("idEdificio"),
                    asset.getString("numeroEmpleado")
            );

        }

        Toast.makeText(
                MainActivity.this,
                "Assets: " + assets.length(),
                Toast.LENGTH_LONG
        ).show();

        dbHandler.clearAssets();
        dbHandler.addAssets(assetsArray);
        setAssets(assetsArray);

        updateSyncButtonsText();
    }
}
