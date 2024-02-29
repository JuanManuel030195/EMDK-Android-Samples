package com.symbol.barcodesample1.controllers;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.symbol.barcodesample1.Asset;
import com.symbol.barcodesample1.AssetPerValidation;
import com.symbol.barcodesample1.LocalValidation;
import com.symbol.barcodesample1.MainActivity;
import com.symbol.barcodesample1.R;
import com.symbol.barcodesample1.SentState;

public class OldValidationsController extends Activity {

    private void addTableRowToOldValidationsTable(LocalValidation oldValidation, View view) {
        Button button = new Button(MainActivity.this);
        button.setText(R.string.continuar);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadOldValidation(oldValidation, view);
            }
        });

        TextView oldValidationDetailsTextView = new TextView(MainActivity.this);
        String oldValidationDetails = oldValidation.getDate().toString();
        oldValidationDetails += "\n" + oldValidation.getEmployeeName();
        oldValidationDetails += "\n" + oldValidation.getBuilding().getName();
        oldValidationDetailsTextView.setText(oldValidationDetails);

        TableRow newTableRow = new TableRow(MainActivity.this);
        newTableRow.addView(button);
        newTableRow.addView(oldValidationDetailsTextView);

        TableLayout tableLayout = (TableLayout) findViewById(R.id.oldValidationsTable);
        tableLayout.addView(newTableRow);
    }

    public void getOldValidations(View view) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TableLayout tableLayout = (TableLayout) findViewById(R.id.oldValidationsTable);
                tableLayout.removeAllViews();

                LocalValidation[] localValidations = getOldValidations(SentState.NOT_SENT);
                for (LocalValidation localValidation : localValidations) {
                    if (
                            currentEmployee.getLevel() != 2 &&
                                    !localValidation.getEmployeeNumber().equals(currentEmployee.getNumber())
                    ) {
                        continue;
                    }

                    addTableRowToOldValidationsTable(localValidation, view);
                }
            }
        });
    }

    public void loadOldValidation(LocalValidation oldValidation, View view) {
        clearAllTableRowsFromTableLayout();

        this.currentValidation = oldValidation;

        this.assets = dbHandler.getAssetsByValidation(oldValidation);
        this.currentAssetsPerValidation = dbHandler.getAssetsPerValidationByValidationId(oldValidation.getId());
        for (AssetPerValidation assetPerValidation : this.currentAssetsPerValidation) {
            Asset currentAsset = dbHandler.getAssetByNumber(
                    assetPerValidation.getAssetNumber()
            );

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addTableRowToTableLayout(currentAsset);
                    updateTableRowColor(
                            currentAsset.getNumber(),
                            assetPerValidation.getStatus()
                    );
                }
            });
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        MainActivity.this,
                        currentValidation.getBuilding().toString(),
                        Toast.LENGTH_LONG
                ).show();

                initScanner();
                softScan(view);
            }
        });
    }

    public LocalValidation[] getOldValidations(SentState sentState) {
        return dbHandler.getOldValidations(sentState);
    }

}
