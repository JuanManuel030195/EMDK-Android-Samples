/*
 * Copyright (C) 2015-2019 Zebra Technologies Corporation and/or its affiliates
 * All rights reserved.
 */
package com.symbol.barcodesample1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ConnectionState;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.Scanner.TriggerType;
import com.symbol.emdk.barcode.StatusData.ScannerStates;
import com.symbol.emdk.barcode.StatusData;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.pm.ActivityInfo;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener, OnCheckedChangeListener {

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;

    private TextView textViewData = null;
    private TextView textViewStatus = null;

    private CheckBox checkBoxEAN8 = null;
    private CheckBox checkBoxEAN13 = null;
    private CheckBox checkBoxCode39 = null;
    private CheckBox checkBoxCode128 = null;

    private Spinner spinnerScannerDevices = null;

    private List<ScannerInfo> deviceList = null;

    private int scannerIndex = 0; // Keep the selected scanner
    private int defaultIndex = 0; // Keep the default scanner
    private int dataLength = 0;
    private String statusString = "";

    private boolean bSoftTriggerSelected = false;
    private boolean bDecoderSettingsChanged = false;
    private boolean bExtScannerDisconnected = false;
    private final Object lock = new Object();

    private DBHandler dbHandler;

    private final MediaType jsonMediaType = MediaType.parse("application/json; charset=utf-8");
    private final String baseUrl = "http://cda.ceaqueretaro.gob.mx/";

    private String username = "";
    private String password = "";

    private Employee[] employees = new Employee[0];
    private Building[] buildings = new Building[0];
    private Asset[] assets = new Asset[0];

    private LocalValidation currentValidation = null;
    private AssetPerValidation[] currentAssetsPerValidation = new AssetPerValidation[0];

    private AppState appState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceList = new ArrayList<ScannerInfo>();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setDefaultOrientation();

        dbHandler = new DBHandler(MainActivity.this);

        setEmployees(dbHandler.getEmployees());
        setEmployeesToSpinner();


        setBuildings(dbHandler.getBuildings());
        setBuildingsToSpinner();

        setAssets(dbHandler.getAssets());

        textViewData = (TextView)findViewById(R.id.textViewData);
        textViewStatus = (TextView)findViewById(R.id.textViewStatus);
        checkBoxEAN8 = (CheckBox)findViewById(R.id.checkBoxEAN8);
        checkBoxEAN13 = (CheckBox)findViewById(R.id.checkBoxEAN13);
        checkBoxCode39 = (CheckBox)findViewById(R.id.checkBoxCode39);
        checkBoxCode128 = (CheckBox)findViewById(R.id.checkBoxCode128);
        spinnerScannerDevices = (Spinner)findViewById(R.id.spinnerScannerDevices);

        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            updateStatus("EMDKManager object request failed!");
            return;
        }

        checkBoxEAN8.setOnCheckedChangeListener(this);
        checkBoxEAN13.setOnCheckedChangeListener(this);
        checkBoxCode39.setOnCheckedChangeListener(this);
        checkBoxCode128.setOnCheckedChangeListener(this);

        addSpinnerScannerDevicesListener();

        textViewData.setSelected(true);
        textViewData.setMovementMethod(new ScrollingMovementMethod());

        appState = AppState.NOT_LOGGED_IN;
        updateVisualComponentsBasedOnAppState(appState);

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
    }

    public void updateTableRowColor(String assetNumber, ValidationStatus status) {
        TableLayout tableLayout = (TableLayout) findViewById(R.id.scannedAssetsTable);
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            TableRow tableRow = (TableRow) tableLayout.getChildAt(i);
            TextView assetNumberTextView = (TextView) tableRow.getChildAt(0);
            if (assetNumberTextView.getText().toString().equals(assetNumber)) {
                switch (status) {
                    case PENDING:
                        tableRow.setBackgroundColor(getColor(R.color.colorPending));
                        break;
                    case OK:
                        tableRow.setBackgroundColor(getColor(R.color.colorOk));
                        break;
                    case WRONG_EMPLOYEE:
                        tableRow.setBackgroundColor(getColor(R.color.colorWrongEmployee));
                        break;
                    case WRONG_BUILDING:
                        tableRow.setBackgroundColor(getColor(R.color.colorWrongBuilding));
                        break;
                }
                break;
            }
        }
    }

    private void addTableRowToTableLayout(Asset asset) {
        TextView newAssetNumberTextView = new TextView(MainActivity.this);
        newAssetNumberTextView.setText(asset.getNumber());

        TextView newAssetDescriptionTextView = new TextView(MainActivity.this);
        newAssetDescriptionTextView.setText(asset.getDescription());

        TableRow newTableRow = new TableRow(MainActivity.this);
        newTableRow.addView(newAssetNumberTextView);
        newTableRow.addView(newAssetDescriptionTextView);

        TableLayout tableLayout = (TableLayout) findViewById(R.id.scannedAssetsTable);
        tableLayout.addView(newTableRow);
    }

    private void removeTableRowFromTableLayout(Asset asset) {
        TableLayout tableLayout = (TableLayout) findViewById(R.id.scannedAssetsTable);
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            TableRow tableRow = (TableRow) tableLayout.getChildAt(i);
            TextView assetNumberTextView = (TextView) tableRow.getChildAt(0);
            if (assetNumberTextView.getText().toString().equals(asset.getNumber())) {
                tableLayout.removeView(tableRow);
                break;
            }
        }
    }

    private void setEmployeesToSpinner() {
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.employeeSpinner);

        ArrayList<String> employeeNames = new ArrayList<String>();
        for (Employee employee : this.employees) {
            employeeNames.add(employee.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                MainActivity.this,
                android.R.layout.simple_dropdown_item_1line,
                employeeNames
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoCompleteTextView.setAdapter(adapter);
    }

    private void setBuildingsToSpinner() {
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.buildingSpinner);

        ArrayList<String> buildingNames = new ArrayList<String>();
        for (Building building : this.buildings) {
            buildingNames.add(building.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                MainActivity.this,
                android.R.layout.simple_dropdown_item_1line,
                buildingNames
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoCompleteTextView.setAdapter(adapter);
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        updateStatus("EMDK open success!");
        this.emdkManager = emdkManager;
        // Acquire the barcode manager resources
        initBarcodeManager();
        // Enumerate scanner devices
        enumerateScannerDevices();
        // Set default scanner
        spinnerScannerDevices.setSelection(defaultIndex);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The application is in foreground
        if (emdkManager != null) {
            // Acquire the barcode manager resources
            initBarcodeManager();
            // Enumerate scanner devices
            enumerateScannerDevices();
            // Set selected scanner
            spinnerScannerDevices.setSelection(scannerIndex);
            // Initialize scanner
            initScanner();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The application is in background
        // Release the barcode manager resources
        deInitScanner();
        deInitBarcodeManager();
    }

    @Override
    public void onClosed() {
        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
        updateStatus("EMDK closed unexpectedly! Please close and restart the application.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    private void updateValidationData(String assetNumberReed) {
        Asset asset = null;
        for (Asset localAsset : this.assets) {
            if (localAsset.getNumber().equals(assetNumberReed)) {
                asset = localAsset;
                break;
            }
        }

        if (asset == null) {
            return;
        }

        AssetPerValidation assetPerValidation = null;
        for (AssetPerValidation localAssetPerValidation : this.currentAssetsPerValidation) {
            if (localAssetPerValidation.getAssetNumber().equals(assetNumberReed)) {
                assetPerValidation = localAssetPerValidation;
                break;
            }
        }

        if (assetPerValidation == null) {
            return;
        }

        assetPerValidation.setScanned(true);
        assetPerValidation.setStatus(
            asset,
            this.currentValidation.getEmployee(),
            this.currentValidation.getBuilding()
        );

        dbHandler.updateAssetPerValidation(assetPerValidation);
        updateTableRowColor(asset.getNumber(), assetPerValidation.getStatus());
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList <ScanData> scanData = scanDataCollection.getScanData();
            for(ScanData data : scanData) {
                updateData("<font color='gray'>" + data.getLabelType() + "</font> : " + data.getData());
                updateValidationData(data.getData());
            }
        }
    }

    @Override
    public void onStatus(StatusData statusData) {
        ScannerStates state = statusData.getState();
        switch(state) {
            case IDLE:
                statusString = statusData.getFriendlyName()+" is enabled and idle...";
                updateStatus(statusString);
                // set trigger type
                if(bSoftTriggerSelected) {
                    scanner.triggerType = TriggerType.SOFT_ONCE;
                    bSoftTriggerSelected = false;
                } else {
                    scanner.triggerType = TriggerType.HARD;
                }
                // set decoders
                if(bDecoderSettingsChanged) {
                    setDecoders();
                    bDecoderSettingsChanged = false;
                }
                // submit read
                if(!scanner.isReadPending() && !bExtScannerDisconnected) {
                    try {
                        scanner.read();
                    } catch (ScannerException e) {
                        updateStatus(e.getMessage());
                    }
                }
                break;
            case WAITING:
                statusString = "Scanner is waiting for trigger press...";
                updateStatus(statusString);
                break;
            case SCANNING:
                statusString = "Scanning...";
                updateStatus(statusString);
                break;
            case DISABLED:
                statusString = statusData.getFriendlyName()+" is disabled.";
                updateStatus(statusString);
                break;
            case ERROR:
                statusString = "An error has occurred.";
                updateStatus(statusString);
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, ConnectionState connectionState) {
        String status;
        String scannerName = "";
        String statusExtScanner = connectionState.toString();
        String scannerNameExtScanner = scannerInfo.getFriendlyName();
        if (deviceList.size() != 0) {
            scannerName = deviceList.get(scannerIndex).getFriendlyName();
        }
        if (scannerName.equalsIgnoreCase(scannerNameExtScanner)) {
            switch(connectionState) {
                case CONNECTED:
                    bSoftTriggerSelected = false;
                    synchronized (lock) {
                        initScanner();
                        bExtScannerDisconnected = false;
                    }
                    break;
                case DISCONNECTED:
                    bExtScannerDisconnected = true;
                    synchronized (lock) {
                        deInitScanner();
                    }
                    break;
            }
            status = scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status);
        }
        else {
            bExtScannerDisconnected = false;
            status =  statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status);
        }
    }

    private void initScanner() {
        if (scanner == null) {
            if ((deviceList != null) && (deviceList.size() != 0)) {
                if (barcodeManager != null)
                    scanner = barcodeManager.getDevice(deviceList.get(scannerIndex));
            }
            else {
                updateStatus("Failed to get the specified scanner device! Please close and restart the application.");
                return;
            }
            if (scanner != null) {
                scanner.addDataListener(this);
                scanner.addStatusListener(this);
                try {
                    scanner.enable();
                } catch (ScannerException e) {
                    updateStatus(e.getMessage());
                    deInitScanner();
                }
            }else{
                updateStatus("Failed to initialize the scanner device.");
            }
        }
    }

    private void deInitScanner() {
        if (scanner != null) {
            try{
                scanner.disable();
            } catch (Exception e) {
                updateStatus(e.getMessage());
            }

            try {
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
            } catch (Exception e) {
                updateStatus(e.getMessage());
            }

            try{
                scanner.release();
            } catch (Exception e) {
                updateStatus(e.getMessage());
            }
            scanner = null;
        }
    }

    private void initBarcodeManager(){
        barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);
        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);
        }
    }

    private void deInitBarcodeManager(){
        if (emdkManager != null) {
            emdkManager.release(FEATURE_TYPE.BARCODE);
        }
    }

    private void addSpinnerScannerDevicesListener() {
        spinnerScannerDevices.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1, int position, long arg3) {
                if ((scannerIndex != position) || (scanner==null)) {
                    scannerIndex = position;
                    bSoftTriggerSelected = false;
                    bExtScannerDisconnected = false;
                    deInitScanner();
                    initScanner();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void enumerateScannerDevices() {
        if (barcodeManager != null) {
            List<String> friendlyNameList = new ArrayList<String>();
            int spinnerIndex = 0;
            deviceList = barcodeManager.getSupportedDevicesInfo();
            if ((deviceList != null) && (deviceList.size() != 0)) {
                Iterator<ScannerInfo> it = deviceList.iterator();
                while(it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    friendlyNameList.add(scnInfo.getFriendlyName());
                    if(scnInfo.isDefaultScanner()) {
                        defaultIndex = spinnerIndex;
                    }
                    ++spinnerIndex;
                }
            }
            else {
                updateStatus("Failed to get the list of supported scanner devices! Please close and restart the application.");
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, friendlyNameList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerScannerDevices.setAdapter(spinnerAdapter);
        }
    }

    private void setDecoders() {
        if (scanner != null) {
            try {
                ScannerConfig config = scanner.getConfig();
                // Set EAN8
                config.decoderParams.ean8.enabled = checkBoxEAN8.isChecked();
                // Set EAN13
                config.decoderParams.ean13.enabled = checkBoxEAN13.isChecked();
                // Set Code39
                config.decoderParams.code39.enabled= checkBoxCode39.isChecked();
                //Set Code128
                config.decoderParams.code128.enabled = checkBoxCode128.isChecked();
                scanner.setConfig(config);
            } catch (ScannerException e) {
                updateStatus(e.getMessage());
            }
        }
    }

    public void setAssets(Asset[] assets) {
        this.assets = assets;
    }

    public void getAssets(View view) {
        TextView textViewLoginStatus = (TextView) findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(R.string.sync_with_server_progress_text);
        textViewLoginStatus.setVisibility(View.VISIBLE);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("numeroEmpleado", this.username);
            requestBody.put("password", this.password);
        } catch (JSONException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
            return;
        }

        String endPoint = "index.php?r=auth%2Fbackup";
        try {
            postRequest(
                    endPoint,
                    requestBody,
                    (IOException e) -> {
                        textViewLoginStatus.setText(R.string.sync_error_text);
                        textViewLoginStatus.setVisibility(View.VISIBLE);
                        return null;
                    },
                    (JSONObject responseBody) -> {
                        try {
                            if (
                                !responseBody.has("success") ||
                                !responseBody.getBoolean("success")
                            ) {
                                textViewLoginStatus.setText(R.string.sync_error_text);
                                textViewLoginStatus.setVisibility(View.VISIBLE);
                                return null;
                            }

                            if (!responseBody.has("activos")) {
                                textViewLoginStatus.setText(R.string.sync_error_text);
                                textViewLoginStatus.setVisibility(View.VISIBLE);
                                return null;
                            }

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

                        } catch (JSONException e) {
                            textViewLoginStatus.setText(e.getMessage());
                            textViewLoginStatus.setVisibility(View.VISIBLE);
                        }

                        return null;
                    },
                    (JSONException e) -> {
                        textViewLoginStatus.setText(e.getMessage());
                        textViewLoginStatus.setVisibility(View.VISIBLE);
                        return null;
                    }
            );
        } catch (IOException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
        }
    }

    public void setEmployees(Employee[] employees) {
        this.employees = employees;
    }

    public void getEmployees(View view) {
        TextView textViewLoginStatus = (TextView) findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(R.string.sync_with_server_progress_text);
        textViewLoginStatus.setVisibility(View.VISIBLE);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("numeroEmpleado", this.username);
            requestBody.put("password", this.password);
        } catch (JSONException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
            return;
        }

        String endPoint = "index.php?r=auth%2Fempleados";
        try {
            postRequest(
                    endPoint,
                    requestBody,
                    (IOException e) -> {
                        onPostFailed(e);
                        return null;
                    },
                    (JSONObject responseBody) -> {
                        try {
                            if (
                                !responseBody.has("success") ||
                                !responseBody.getBoolean("success")
                            ) {
                                return null;
                            }

                            if (!responseBody.has("employees")) {
                                return null;
                            }

                            textViewLoginStatus.setVisibility(View.GONE);

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

                            dbHandler.clearEmployees();
                            dbHandler.addEmployees(employeesArray);
                            setEmployees(employeesArray);
                            setEmployeesToSpinner();

                            updateSyncButtonsText();

                        } catch (JSONException e) {
                            textViewLoginStatus.setText(e.getMessage());
                            textViewLoginStatus.setVisibility(View.VISIBLE);
                        }

                        return null;
                    },
                    (JSONException e) -> {
                        textViewLoginStatus.setText(e.getMessage());
                        textViewLoginStatus.setVisibility(View.VISIBLE);
                        return null;
                    }
            );
        } catch (IOException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
        }
    }

    public void setBuildings(Building[] buildings) {
        this.buildings = buildings;
    }

    public void getBuildings(View view) {
        TextView textViewLoginStatus = (TextView) findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(R.string.sync_with_server_progress_text);
        textViewLoginStatus.setVisibility(View.VISIBLE);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("numeroEmpleado", this.username);
            requestBody.put("password", this.password);
        } catch (JSONException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
            return;
        }

        String endPoint = "index.php?r=auth%2Fedificios";
        try {
            postRequest(
                    endPoint,
                    requestBody,
                    (IOException e) -> {
                        onPostFailed(e);
                        return null;
                    },
                    (JSONObject responseBody) -> {
                        try {
                            if (
                                !responseBody.has("success") ||
                                !responseBody.getBoolean("success")
                            ) {
                                return null;
                            }

                            if (!responseBody.has("buildings")) {
                                return null;
                            }

                            textViewLoginStatus.setVisibility(View.GONE);

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

                        } catch (JSONException e) {
                            textViewLoginStatus.setText(e.getMessage());
                            textViewLoginStatus.setVisibility(View.VISIBLE);
                        }

                        return null;
                    },
                    (JSONException e) -> {
                        textViewLoginStatus.setText(e.getMessage());
                        textViewLoginStatus.setVisibility(View.VISIBLE);
                        return null;
                    }
            );
        } catch (IOException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
        }
    }

    public void logOut(View view) {
        TextView textViewLoginStatus = (TextView) findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(R.string.login_progress_signing_in_text);
        textViewLoginStatus.setVisibility(View.VISIBLE);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("numeroEmpleado", this.username);
            requestBody.put("password", this.password);
        } catch (JSONException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
            return;
        }

        String endPoint = "index.php?r=auth%2Flogout";
        try {
            postRequest(
                endPoint,
                requestBody,
                (IOException e) -> {
                    onPostFailed(e);
                    return null;
                },
                (JSONObject responseBody) -> {
                    try {
                        if (
                            responseBody.has("success") &&
                            responseBody.getBoolean("success")
                        ) {
                            appState = AppState.NOT_LOGGED_IN;
                            updateVisualComponentsBasedOnAppState(appState);
                        }
                    } catch (JSONException e) {
                        textViewLoginStatus.setText(e.getMessage());
                        textViewLoginStatus.setVisibility(View.VISIBLE);
                    }

                    return null;
                },
                (JSONException e) -> {
                    textViewLoginStatus.setText(e.getMessage());
                    textViewLoginStatus.setVisibility(View.VISIBLE);
                    return null;
                }
            );
        } catch (IOException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
        }
    }

    public void login(View view) {
        getUserName();
        getPassword();

        TextView textViewLoginStatus = (TextView) findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(R.string.login_progress_signing_in_text);
        textViewLoginStatus.setVisibility(View.VISIBLE);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("numeroEmpleado", this.username);
            requestBody.put("password", this.password);
        } catch (JSONException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
            return;
        }

        String endPoint = "index.php?r=auth%2Flogin";
        try {
            postRequest(
                endPoint,
                requestBody,
                (IOException e) -> {
                    onLoginFailed(e);
                    return null;
                },
                (JSONObject responseBody) -> {
                    try {
                        if (
                            responseBody.has("success") &&
                            responseBody.getBoolean("success")
                        ) {
                            appState = AppState.LOGGED_IN;
                            updateVisualComponentsBasedOnAppState(appState);
                            return null;
                        }

                        textViewLoginStatus.setText(R.string.login_error_text);
                        textViewLoginStatus.setVisibility(View.VISIBLE);
                    } catch (JSONException e) {
                        textViewLoginStatus.setText(e.getMessage());
                        textViewLoginStatus.setVisibility(View.VISIBLE);
                    }

                    return null;
                },
                (JSONException e) -> {
                    textViewLoginStatus.setText(e.getMessage());
                    textViewLoginStatus.setVisibility(View.VISIBLE);
                    return null;
                }
            );
        } catch (IOException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
        }

    }

    public String saveStringToFile(String fileName, String string) throws IOException {
        FileOutputStream fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
        fileOutputStream.write(string.getBytes());
        fileOutputStream.close();

        String filePath = getFilesDir().getAbsolutePath() + "/" + fileName;

        // add execute permission to the file
        String[] command = {"chmod", "700", filePath};
        Runtime.getRuntime().exec(command);

        return filePath;
    }

    public void startValidation(View view) {
        AutoCompleteTextView employeeTextView = (AutoCompleteTextView) findViewById(R.id.employeeSpinner);
        String employeeName = employeeTextView.getText().toString().trim();
        Employee employee = dbHandler.getEmployeeByName(employeeName);

        AutoCompleteTextView buildingTextView = (AutoCompleteTextView) findViewById(R.id.buildingSpinner);
        String buildingName = buildingTextView.getText().toString().trim();
        Building building = dbHandler.getBuildingByName(buildingName);

        LocalValidation localValidation = new LocalValidation(
                employee,
                building
        );

        int id = (int) dbHandler.addValidation(localValidation);
        localValidation.setId(id);

        this.currentValidation = localValidation;

        Asset[] assets = dbHandler.getAssetsByValidation(localValidation);
        this.assets = assets;
        dbHandler.addAssetsPerValidation(localValidation, assets);
        this.currentAssetsPerValidation = new AssetPerValidation[assets.length];
        for (int i = 0; i < assets.length; i++) {
            this.currentAssetsPerValidation[i] = new AssetPerValidation(
                    localValidation.getId(),
                    assets[i].getNumber()
            );

            addTableRowToTableLayout(assets[i]);
            updateTableRowColor(assets[i].getNumber(), ValidationStatus.PENDING);
        }

        appState = AppState.VALIDATION_STARTED;
        updateVisualComponentsBasedOnAppState(appState);

        initScanner();
        softScan(view);
    }

    public void closeValidation(View view) {
        appState = AppState.VALIDATION_ENDED;
        updateVisualComponentsBasedOnAppState(appState);
    }

    public void loadOldValidation(int validationId) {
        this.currentValidation = dbHandler.getValidationById(validationId);
        this.currentAssetsPerValidation = dbHandler.getAssetsPerValidationByValidationId(validationId);
        this.assets = new Asset[this.currentAssetsPerValidation.length];
        for (int i = 0; i < this.currentAssetsPerValidation.length; i++) {
            this.assets[i] = dbHandler.getAssetByNumber(
                this.currentAssetsPerValidation[i].getAssetNumber()
            );
        }
    }

    public LocalValidation[] getOldValidations(SentState sentState) {
        return dbHandler.getOldValidations(sentState);
    }

    public void sendValidation(View view) {
        TextView textViewLoginStatus = (TextView) findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(R.string.sync_with_server_progress_text);
        textViewLoginStatus.setVisibility(View.VISIBLE);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("numeroEmpleado", this.username);
            requestBody.put("password", this.password);
            requestBody.put("currentValidation", this.currentValidation);
            requestBody.put("assets", this.assets);
            requestBody.put("currentAssetsPerValidation", this.currentAssetsPerValidation);
        } catch (JSONException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
            return;
        }

        String endPoint = "index.php?r=auth%2Fconfronta";
        try {
            postRequest(
                    endPoint,
                    requestBody,
                    (IOException e) -> {
                        onPostFailed(e);
                        return null;
                    },
                    (JSONObject responseBody) -> {
                        try {
                            if (
                                !responseBody.has("success") ||
                                !responseBody.getBoolean("success")
                            ) {
                                return null;
                            }

                            // On logged in, by the moment
                            appState = AppState.LOGGED_IN;
                            updateVisualComponentsBasedOnAppState(appState);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Confronta física enviada!",
                                    Toast.LENGTH_LONG
                            ).show();

                        } catch (JSONException e) {
                            textViewLoginStatus.setText(e.getMessage());
                            textViewLoginStatus.setVisibility(View.VISIBLE);
                        }

                        return null;
                    },
                    (JSONException e) -> {
                        textViewLoginStatus.setText(e.getMessage());
                        textViewLoginStatus.setVisibility(View.VISIBLE);
                        return null;
                    }
            );
        } catch (IOException e) {
            textViewLoginStatus.setText(R.string.sync_error_text);
            System.out.println(e.getMessage());
        }
    }

    private void updateVisualComponentsBasedOnAppState(AppState state) {
        TextView userNameLabel = (TextView) findViewById(R.id.userNameLabel);
        EditText userNameEditText = (EditText) findViewById(R.id.userName);
        TextView passwordLabel = (TextView) findViewById(R.id.passwordLabel);
        EditText passwordEditText = (EditText) findViewById(R.id.password);
        TextView textViewLoginStatus = (TextView) findViewById(R.id.loginProgress);
        Button loginButton = (Button) findViewById(R.id.loginButton);
        Button logOutButton = (Button) findViewById(R.id.logOutButton);

        Button syncWithServerButton = (Button) findViewById(R.id.syncWithServerButton);
        Button getEmployeesButton = (Button) findViewById(R.id.syncEmployeesWithServerButton);
        Button getBuildingsButton = (Button) findViewById(R.id.syncBuildingsWithServerButton);

        AutoCompleteTextView employeeSpinner = (AutoCompleteTextView) findViewById(R.id.employeeSpinner);
        AutoCompleteTextView buildingSpinner = (AutoCompleteTextView) findViewById(R.id.buildingSpinner);

        Button startValidationButton = (Button) findViewById(R.id.startValidationButton);
        Button closeValidationButton = (Button) findViewById(R.id.closeValidationButton);
        Button sendValidationButton = (Button) findViewById(R.id.sendValidationButton);

        TableLayout scannedAssetsTable = (TableLayout) findViewById(R.id.scannedAssetsTable);
        Button buttonScan = (Button) findViewById(R.id.buttonScan);

        switch (state) {
            case NOT_LOGGED_IN:
                userNameLabel.setText(R.string.user_name_label_text);
                userNameLabel.setVisibility(View.VISIBLE);
                userNameEditText.setVisibility(View.VISIBLE);
                passwordLabel.setText(R.string.password_label_text);
                passwordLabel.setVisibility(View.VISIBLE);
                passwordEditText.setVisibility(View.VISIBLE);
                textViewLoginStatus.setVisibility(View.GONE);
                textViewLoginStatus.setText("");
                loginButton.setText(R.string.login_button_text);
                loginButton.setVisibility(View.VISIBLE);
                logOutButton.setVisibility(View.GONE);

                syncWithServerButton.setVisibility(View.GONE);
                getEmployeesButton.setVisibility(View.GONE);
                getBuildingsButton.setVisibility(View.GONE);

                employeeSpinner.setVisibility(View.GONE);
                buildingSpinner.setVisibility(View.GONE);

                startValidationButton.setVisibility(View.GONE);
                closeValidationButton.setVisibility(View.GONE);
                sendValidationButton.setVisibility(View.GONE);

                scannedAssetsTable.removeAllViews();
                scannedAssetsTable.setVisibility(View.GONE);
                buttonScan.setVisibility(View.GONE);
                break;
            case LOGGED_IN:
                boolean isEmployeeInLocal = dbHandler.isEmployeeInDB(this.username);
                String userNameLabelText = isEmployeeInLocal
                    ? dbHandler.getEmployeeByNumber(this.username).getName()
                    : "Usuario: " + this.username + " (no local)";
                userNameLabel.setText(userNameLabelText);
                userNameLabel.setVisibility(View.VISIBLE);
                userNameEditText.setText("");
                userNameEditText.setVisibility(View.GONE);
                passwordLabel.setVisibility(View.GONE);
                passwordEditText.setText("");
                passwordEditText.setVisibility(View.GONE);
                textViewLoginStatus.setVisibility(View.GONE);
                textViewLoginStatus.setText("");
                loginButton.setVisibility(View.GONE);
                logOutButton.setVisibility(View.VISIBLE);

                syncWithServerButton.setVisibility(View.VISIBLE);
                getEmployeesButton.setVisibility(View.VISIBLE);
                getBuildingsButton.setVisibility(View.VISIBLE);

                employeeSpinner.setVisibility(View.VISIBLE);
                buildingSpinner.setVisibility(View.VISIBLE);

                startValidationButton.setVisibility(View.VISIBLE);
                closeValidationButton.setVisibility(View.GONE);
                sendValidationButton.setVisibility(View.GONE);

                scannedAssetsTable.setVisibility(View.GONE);
                buttonScan.setVisibility(View.GONE);
                break;
            case VALIDATION_STARTED:
                String validationInfo = getResources().getString(R.string.confronta_f_sica_en_proceso);
                validationInfo += "\r\n Empleado: " + this.currentValidation.getEmployeeName();
                validationInfo += "\r\n " + this.currentValidation.getBuildingName();

                userNameLabel.setText(validationInfo);
                userNameLabel.setVisibility(View.VISIBLE);
                userNameEditText.setVisibility(View.GONE);
                passwordLabel.setVisibility(View.GONE);
                passwordEditText.setVisibility(View.GONE);
                textViewLoginStatus.setVisibility(View.GONE);
                textViewLoginStatus.setText("");
                loginButton.setVisibility(View.GONE);
                logOutButton.setVisibility(View.GONE);

                syncWithServerButton.setVisibility(View.GONE);
                getEmployeesButton.setVisibility(View.GONE);
                getBuildingsButton.setVisibility(View.GONE);

                employeeSpinner.setVisibility(View.GONE);
                buildingSpinner.setVisibility(View.GONE);

                startValidationButton.setVisibility(View.GONE);
                closeValidationButton.setVisibility(View.VISIBLE);
                sendValidationButton.setVisibility(View.GONE);

                scannedAssetsTable.setVisibility(View.VISIBLE);
                buttonScan.setVisibility(View.VISIBLE);
                break;
            case VALIDATION_ENDED:
                userNameLabel.setText(R.string.confronta_f_sica_termianda);
                userNameLabel.setVisibility(View.VISIBLE);
                userNameEditText.setVisibility(View.GONE);
                passwordLabel.setVisibility(View.GONE);
                passwordEditText.setVisibility(View.GONE);
                textViewLoginStatus.setVisibility(View.GONE);
                textViewLoginStatus.setText("");
                loginButton.setVisibility(View.GONE);
                logOutButton.setVisibility(View.GONE);

                syncWithServerButton.setVisibility(View.GONE);
                getEmployeesButton.setVisibility(View.GONE);
                getBuildingsButton.setVisibility(View.GONE);

                employeeSpinner.setVisibility(View.GONE);
                buildingSpinner.setVisibility(View.GONE);

                startValidationButton.setVisibility(View.GONE);
                closeValidationButton.setVisibility(View.GONE);
                sendValidationButton.setVisibility(View.VISIBLE);

                scannedAssetsTable.setVisibility(View.VISIBLE);
                buttonScan.setVisibility(View.GONE);
                break;
            case ON_OLD_VALIDATIONS:
                userNameLabel.setText(R.string.confrontas_f_sicas_pendientes);
                userNameLabel.setVisibility(View.VISIBLE);
                userNameEditText.setVisibility(View.GONE);
                passwordLabel.setVisibility(View.GONE);
                passwordEditText.setVisibility(View.GONE);
                textViewLoginStatus.setVisibility(View.GONE);
                textViewLoginStatus.setText("");
                loginButton.setVisibility(View.GONE);
                logOutButton.setVisibility(View.GONE);

                syncWithServerButton.setVisibility(View.GONE);
                getEmployeesButton.setVisibility(View.GONE);
                getBuildingsButton.setVisibility(View.GONE);

                employeeSpinner.setVisibility(View.GONE);
                buildingSpinner.setVisibility(View.GONE);

                startValidationButton.setVisibility(View.GONE);
                closeValidationButton.setVisibility(View.GONE);
                sendValidationButton.setVisibility(View.GONE);

                scannedAssetsTable.setVisibility(View.GONE);
                buttonScan.setVisibility(View.GONE);
                break;
            case ON_OLD_VALIDATION:
                userNameLabel.setText(R.string.confronta_f_sica_pendiente);
                userNameLabel.setVisibility(View.VISIBLE);
                userNameEditText.setVisibility(View.GONE);
                passwordLabel.setVisibility(View.GONE);
                passwordEditText.setVisibility(View.GONE);
                textViewLoginStatus.setVisibility(View.GONE);
                textViewLoginStatus.setText("");
                loginButton.setVisibility(View.GONE);
                logOutButton.setVisibility(View.GONE);

                syncWithServerButton.setVisibility(View.GONE);
                getEmployeesButton.setVisibility(View.GONE);
                getBuildingsButton.setVisibility(View.GONE);

                employeeSpinner.setVisibility(View.GONE);
                buildingSpinner.setVisibility(View.GONE);

                startValidationButton.setVisibility(View.GONE);
                closeValidationButton.setVisibility(View.GONE);
                sendValidationButton.setVisibility(View.VISIBLE);

                scannedAssetsTable.setVisibility(View.VISIBLE);
                buttonScan.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void softScan(View view) {
        bSoftTriggerSelected = true;
        cancelRead();
    }

    private void onPostFailed(IOException e) {
        TextView textViewLoginStatus = (TextView) findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(e.getMessage());
        textViewLoginStatus.setVisibility(View.VISIBLE);
    }

    private void onLoginFailed(IOException e) {
        TextView textViewLoginStatus = (TextView) findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(e.getMessage());
        textViewLoginStatus.setVisibility(View.VISIBLE);

        this.password = "";
        EditText passwordEditText = (EditText) findViewById(R.id.password);
        passwordEditText.setText(this.password);
    }

    private void postRequest(
        String endPoint,
        @NotNull JSONObject requestBody,
        Function<IOException, Void> onFail,
        Function<JSONObject, Void> onResponse,
        Function<JSONException, Void> onJsonException
    ) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = baseUrl + endPoint;
        RequestBody body = RequestBody.create(this.jsonMediaType, requestBody.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onFail.apply(e);
                            }
                        });
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final String responseBodyString = response.body().string();
                    final JSONObject responseBody = new JSONObject(responseBodyString);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onResponse.apply(responseBody);
                        }
                    });
                } catch (JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onJsonException.apply(e);
                        }
                    });
                }
            }
        });
    }

    private boolean isValidBackup(String sqlBackup) {
        return sqlBackup.contains("CREATE TABLE") &&
                sqlBackup.contains("INSERT INTO");
    }

    private void getUserName() {
        EditText usernameEditText = (EditText) findViewById(R.id.userName);
        this.username = usernameEditText.getText().toString();
    }

    private void getPassword() {
        EditText passwordEditText = (EditText) findViewById(R.id.password);
        this.password = passwordEditText.getText().toString();
    }

    private void cancelRead(){
        if (scanner != null) {
            if (scanner.isReadPending()) {
                try {
                    scanner.cancelRead();
                } catch (ScannerException e) {
                    updateStatus(e.getMessage());
                }
            }
        }
    }

    private void updateStatus(final String status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText("" + status);
            }
        });
    }

    private void updateData(final String result){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result != null) {
                    if(dataLength ++ > 100) { //Clear the cache after 100 scans
                        textViewData.setText("");
                        dataLength = 0;
                    }
                    textViewData.append(Html.fromHtml(result));
                    textViewData.append("\n");
                    ((View) findViewById(R.id.scrollViewData)).post(new Runnable()
                    {
                        public void run()
                        {
                            ((ScrollView) findViewById(R.id.scrollViewData)).fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }

    private void setDefaultOrientation(){
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        if(width > height){
//            setContentView(R.layout.activity_main_landscape);
            setContentView(R.layout.activity_main);
        } else {
            setContentView(R.layout.activity_main);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
        bDecoderSettingsChanged = true;
        cancelRead();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
