package com.symbol.barcodesample1.controllers;

import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;

import com.symbol.barcodesample1.MainActivity;
import com.symbol.barcodesample1.R;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScannerController implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener {
    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private List<ScannerInfo> deviceList = null;

    private int scannerIndex = 0; // Keep the selected scanner
    private int defaultIndex = 0; // Keep the default scanner
    private int dataLength = 0;
    private String statusString = "";

    private boolean bSoftTriggerSelected = false;
    private boolean bDecoderSettingsChanged = false;
    private boolean bExtScannerDisconnected = false;
    private final Object lock = new Object();

    @Override
    public void onOpened(EMDKManager emdkManager) {
        updateStatus("EMDK open success!");
        this.emdkManager = emdkManager;
        initBarcodeManager(); // Acquire the barcode manager resources
        enumerateScannerDevices(); // Enumerate scanner devices
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
    public void onData(ScanDataCollection scanDataCollection) {
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();
            for(ScanDataCollection.ScanData data : scanData) {
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateValidationData(data.getData());
                    }
                });*/
            }
        }
    }

    @Override
    public void onStatus(StatusData statusData) {
        StatusData.ScannerStates state = statusData.getState();
        switch(state) {
            case IDLE:
                statusString = statusData.getFriendlyName()+" is enabled and idle...";
                updateStatus(statusString);

                // set trigger type
                if(bSoftTriggerSelected) {
                    scanner.triggerType = Scanner.TriggerType.SOFT_ONCE;
                    bSoftTriggerSelected = false;
                } else {
                    scanner.triggerType = Scanner.TriggerType.HARD;
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
    public void onConnectionChange(ScannerInfo scannerInfo, BarcodeManager.ConnectionState connectionState) {
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
        } else {
            bExtScannerDisconnected = false;
            status =  statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status);
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
        barcodeManager = (BarcodeManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);
        }
    }

    private void deInitBarcodeManager(){
        if (emdkManager != null) {
            emdkManager.release(EMDKManager.FEATURE_TYPE.BARCODE);
        }
    }

    /*private void addSpinnerScannerDevicesListener() {
        spinnerScannerDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
    }*/

    private void enumerateScannerDevices() {
        if (barcodeManager == null ) {
            return;
        }

        List<String> friendlyNameList = new ArrayList<String>();

        int spinnerIndex = 0;

        deviceList = barcodeManager.getSupportedDevicesInfo();

        if (deviceList == null || deviceList.size() == 0) {
            updateStatus("Failed to get the list of supported scanner devices! Please close and restart the application.");
            return;
        }


        for (ScannerInfo scnInfo : deviceList) {
            friendlyNameList.add(scnInfo.getFriendlyName());
            if (scnInfo.isDefaultScanner()) {
                defaultIndex = spinnerIndex;
            }
            ++spinnerIndex;
        }

        /*ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, friendlyNameList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerScannerDevices.setAdapter(spinnerAdapter);*/
    }

    private void setDecoders() {
        if (scanner == null) {
            return;
        }

        try {
            ScannerConfig config = scanner.getConfig();
            config.decoderParams.ean8.enabled = true; // Set EAN8
            config.decoderParams.ean13.enabled = true; // Set EAN13
            config.decoderParams.code39.enabled= true; // Set Code39
            config.decoderParams.code128.enabled = true; //Set Code128
            scanner.setConfig(config);
        } catch (ScannerException e) {
            updateStatus(e.getMessage());
        }
    }

    public void softScan() {
        bSoftTriggerSelected = true;
        cancelRead();
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
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText("" + status);
            }
        });*/
    }

    private void initScanner() {
        if (scanner != null) {
            return;
        }

        if (deviceList == null || deviceList.size() == 0) {
            updateStatus("Failed to get the list of supported scanner devices! Please close and restart the application.");
            return;
        }

        if (barcodeManager != null) {
            scanner = barcodeManager.getDevice(deviceList.get(scannerIndex));
        }

        if (scanner == null) {
            updateStatus("Failed to initialize the scanner device.");
            return;
        }

        scanner.addDataListener(this);
        scanner.addStatusListener(this);
        try {
            scanner.enable();
        } catch (ScannerException e) {
            updateStatus(e.getMessage());
            deInitScanner();
        }
    }

    /*private void updateData(final String result){
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
    }*/
}
