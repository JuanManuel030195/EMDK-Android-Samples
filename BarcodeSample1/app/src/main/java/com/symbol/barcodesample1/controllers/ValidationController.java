package com.symbol.barcodesample1.controllers;

import android.app.Activity;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.symbol.barcodesample1.Asset;
import com.symbol.barcodesample1.AssetPerValidation;
import com.symbol.barcodesample1.Building;
import com.symbol.barcodesample1.Employee;
import com.symbol.barcodesample1.LocalValidation;
import com.symbol.barcodesample1.MainActivity;
import com.symbol.barcodesample1.R;
import com.symbol.barcodesample1.ValidationStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ValidationController extends Activity {

    public void updateTableRowColor(String assetNumber, ValidationStatus status) {
        TableLayout tableLayout = (TableLayout) findViewById(R.id.scannedAssetsTable);
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            TableRow tableRow = (TableRow) tableLayout.getChildAt(i);

            TextView assetNumberTextView = (TextView) tableRow.getChildAt(1);

            if (!assetNumberTextView.getText().toString().equals(assetNumber)) {
                continue;
            }

            Button button = (Button) tableRow.getChildAt(0);

            switch (status) {
                case PENDING:
                    button.setVisibility(View.GONE);
                    tableRow.setBackgroundColor(getColor(R.color.colorPending));
                    break;
                case OK:
                    button.setVisibility(View.GONE);
                    tableRow.setBackgroundColor(getColor(R.color.colorOk));
                    break;
                case WRONG_EMPLOYEE:
                    button.setVisibility(View.VISIBLE);
                    tableRow.setBackgroundColor(getColor(R.color.colorWrongEmployee));
                    break;
                case WRONG_BUILDING:
                    button.setVisibility(View.VISIBLE);
                    tableRow.setBackgroundColor(getColor(R.color.colorWrongBuilding));
                    break;
            }

            break;
        }
    }

    private void updateAsset(Asset asset) {
        dbHandler.updateAsset(asset);
    }

    private Button generateIconButtonToUpdateAsset(Asset asset) {
        boolean itBelongsToCurrentValidation = true;
        itBelongsToCurrentValidation &= currentValidation.getEmployeeNumber().equals(asset.getEmployeeNumber());
        itBelongsToCurrentValidation &= currentValidation.getBuilding().getId() == asset.getBuildingId();

        Button button = new Button(MainActivity.this);
        button.setText(getResources().getString(R.string.actualizar));
        button.setVisibility(!itBelongsToCurrentValidation ? View.VISIBLE : View.GONE);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                asset.setBuildingId(currentValidation.getBuilding().getId());
                asset.setEmployeeNumber(currentValidation.getEmployeeNumber());
                updateAsset(asset);
                updateTableRowColor(asset.getNumber(), ValidationStatus.OK);
            }
        });

        return button;
    }

    private void addTableRowToTableLayout(Asset asset) {
        Button button = generateIconButtonToUpdateAsset(asset);

        TextView newAssetNumberTextView = new TextView(MainActivity.this);
        newAssetNumberTextView.setText(asset.getNumber());

        TextView newAssetDescriptionTextView = new TextView(MainActivity.this);
        newAssetDescriptionTextView.setText(asset.getDescription());

        TableRow newTableRow = new TableRow(MainActivity.this);
        newTableRow.addView(button);
        newTableRow.addView(newAssetNumberTextView);
        newTableRow.addView(newAssetDescriptionTextView);

        TableLayout tableLayout = (TableLayout) findViewById(R.id.scannedAssetsTable);
        tableLayout.addView(newTableRow);
    }

    private void clearAllTableRowsFromTableLayout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TableLayout tableLayout = (TableLayout) findViewById(R.id.scannedAssetsTable);
                tableLayout.removeAllViews();
            }
        });
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

    private void addNewAssetPerValidation(AssetPerValidation assetPerValidation) {
        AssetPerValidation[] newCurrentAssetsPerValidation = new AssetPerValidation[this.currentAssetsPerValidation.length + 1];
        System.arraycopy(this.currentAssetsPerValidation, 0, newCurrentAssetsPerValidation, 0, this.currentAssetsPerValidation.length);
        newCurrentAssetsPerValidation[this.currentAssetsPerValidation.length] = assetPerValidation;
        this.currentAssetsPerValidation = newCurrentAssetsPerValidation;
    }

    private void updateValidationData(String assetNumberReed) {
        Toast.makeText(
                MainActivity.this,
                "Número SAP: " + assetNumberReed,
                Toast.LENGTH_LONG
        ).show();

        Asset asset = null;
        for (Asset localAsset : this.assets) {
            if (localAsset.getNumber().equals(assetNumberReed)) {
                asset = localAsset;
                break;
            }
        }

        if (asset == null && dbHandler.isAssetInDB(assetNumberReed)) {
            asset = dbHandler.getAssetByNumber(assetNumberReed);
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
            assetPerValidation = new AssetPerValidation(
                    this.currentValidation.getId(),
                    asset.getNumber()
            );

            dbHandler.addAssetPerValidation(assetPerValidation);
            addNewAssetPerValidation(assetPerValidation);
            addTableRowToTableLayout(asset);
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

    public void startValidation(View view) {
        clearAllTableRowsFromTableLayout();

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

            int finalI = i;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addTableRowToTableLayout(assets[finalI]);
                    updateTableRowColor(assets[finalI].getNumber(), ValidationStatus.PENDING);
                }
            });
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        MainActivity.this,
                        "Validación iniciada!",
                        Toast.LENGTH_LONG
                ).show();


                initScanner();
                softScan(view);
            }
        });
    }

    public void closeValidation(View view) {
        deInitScanner();
    }

    public void sendLocalValidation() {
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

        // call to REST API
    }

    public void onSendLocalValidationSuccess(View view) {
        Toast.makeText(
                MainActivity.this,
                "Confronta física enviada!",
                Toast.LENGTH_LONG
        ).show();
    }

    private void onSendLocalValidationFail(IOException e) {
        Toast.makeText(
                MainActivity.this,
                "Error al enviar la validación",
                Toast.LENGTH_LONG
        ).show();
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
    protected void onDestroy() {
        super.onDestroy();
        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }
}
