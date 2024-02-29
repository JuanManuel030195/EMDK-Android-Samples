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
import android.util.Log;
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

public class MainActivity extends Activity implements OnCheckedChangeListener {

    private static final int ID_LOGOUT = R.id.cerrar_sesi_n;
    private static final int ID_LOGIN = R.id.iniciar_sesion;
    private static final int ID_FETCH_DATA = R.id.action_fetch_data;
    private static final int ID_OLD_VALIDATIONS = R.id.old_validations;
    private static final int ID_NEW_VALIDATION = R.id.new_validation;
    private static final int ID_ASSETS_INFO = R.id.assets_info;

    private DBHandler dbHandler;

    private Employee currentEmployee = null;
    private String username = "";
    private String password = "";


    private Asset[] assets = new Asset[0];

    private LocalValidation currentValidation = null;
    private AssetPerValidation[] currentAssetsPerValidation = new AssetPerValidation[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceList = new ArrayList<ScannerInfo>();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setDefaultOrientation();

        dbHandler = new DBHandler(MainActivity.this);

        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            MainActivity.this,
                            "EMDKManager object request failed!",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        }

        setContentView(R.layout.login_form);
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
        int layoutId = R.layout.activity_main;

        switch (id) {
            case ID_LOGOUT:
                // log out
            case ID_LOGIN:
                layoutId = R.layout.login_form;
                break;
            case ID_FETCH_DATA:
                layoutId = R.layout.fetch_data;
                break;
            case ID_OLD_VALIDATIONS:
                layoutId = R.layout.old_validations;
                break;
            case ID_NEW_VALIDATION:
                layoutId = R.layout.new_validation;
                break;
            case ID_ASSETS_INFO:
                layoutId = R.layout.assets_info;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        setContentView(layoutId);
        return true;
    }
}
