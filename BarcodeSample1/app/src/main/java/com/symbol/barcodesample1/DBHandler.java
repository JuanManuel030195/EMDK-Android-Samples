package com.symbol.barcodesample1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.jetbrains.annotations.NotNull;

public class DBHandler extends SQLiteOpenHelper {
    private static final String DB_NAME = "cda";
    private static final int DB_VERSION = 1;

    private static final String EMPLOYEES_TABLE_NAME = "usuarios";
    private static final String EMPLOYEES_ID_COL = "numeroEmpleado";
    private static final String EMPLOYEES_NAME_COL = "nombre";
    private static final String EMPLOYEES_LEVEL_COL = "nivel";

    private static final String BUILDINGS_TABLE_NAME = "edificios";
    private static final String BUILDINGS_ID_COL = "idEdificio";
    private static final String BUILDINGS_NAME_COL = "nombre";
    private static final String BUILDINGS_NUMBER_COL = "numero";

    private static final String ASSETS_TABLE_NAME = "activos";
    private static final String ASSETS_NUMBER_COL = "numeroSAP";
    private static final String ASSETS_DESCRIPTION_COL = "descripcion";
    private static final String ASSETS_BUILDING_NAME_COL = "edificio";
    private static final String ASSETS_BUILDING_ID_COL = "idEdificio";

    private static final String VALIDATIONS_TABLE_NAME = "validaciones";
    private static final String VALIDATIONS_ID_COL = "id";
    private static final String VALIDATIONS_DATE_COL = "date";
    private static final String VALIDATIONS_EMPLOYEE_NUMBER_COL = "employeeNumber";
    private static final String VALIDATIONS_SENT_STATE_COL = "sentState";

    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createEmployeesTable = "CREATE TABLE " + EMPLOYEES_TABLE_NAME + " (" +
                EMPLOYEES_ID_COL + " TEXT PRIMARY KEY, " +
                EMPLOYEES_NAME_COL + " TEXT, " +
                EMPLOYEES_LEVEL_COL + " INTEGER)";
        db.execSQL(createEmployeesTable);

        String createBuildingsTable = "CREATE TABLE " + BUILDINGS_TABLE_NAME + " (" +
                BUILDINGS_ID_COL + " INTEGER PRIMARY KEY, " +
                BUILDINGS_NAME_COL + " TEXT, " +
                BUILDINGS_NUMBER_COL + " TEXT)";
        db.execSQL(createBuildingsTable);

        String createAssetsTable = "CREATE TABLE " + ASSETS_TABLE_NAME + " (" +
                ASSETS_NUMBER_COL + " TEXT PRIMARY KEY, " +
                ASSETS_DESCRIPTION_COL + " TEXT, " +
                ASSETS_BUILDING_NAME_COL + " TEXT, " +
                ASSETS_BUILDING_ID_COL + " INTEGER)";
        db.execSQL(createAssetsTable);

        String createValidationsTable = "CREATE TABLE " + VALIDATIONS_TABLE_NAME + " (" +
                VALIDATIONS_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                VALIDATIONS_DATE_COL + " DATETIME, " +
                VALIDATIONS_EMPLOYEE_NUMBER_COL + " TEXT, " +
                VALIDATIONS_SENT_STATE_COL + " INTEGER)";
        db.execSQL(createValidationsTable);
    }

    public void addEmployee(@NotNull Employee employee) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(EMPLOYEES_ID_COL, employee.getNumber());
        values.put(EMPLOYEES_NAME_COL, employee.getName());
        values.put(EMPLOYEES_LEVEL_COL, employee.getLevel());
        db.insert(EMPLOYEES_TABLE_NAME, null, values);
        db.close();
    }

    public void addBuilding(@NotNull Building building) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BUILDINGS_ID_COL, building.getId());
        values.put(BUILDINGS_NAME_COL, building.getName());
        values.put(BUILDINGS_NUMBER_COL, building.getNumber());
        db.insert(BUILDINGS_TABLE_NAME, null, values);
        db.close();
    }

    public void addAsset(@NotNull Asset asset) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ASSETS_NUMBER_COL, asset.getNumber());
        values.put(ASSETS_DESCRIPTION_COL, asset.getDescription());
        values.put(ASSETS_BUILDING_NAME_COL, asset.getBuildingName());
        values.put(ASSETS_BUILDING_ID_COL, asset.getBuildingId());
        db.insert(ASSETS_TABLE_NAME, null, values);
        db.close();
    }

    public long addValidation(@NotNull LocalValidation validation) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(VALIDATIONS_DATE_COL, validation.getDate().toString());
        values.put(VALIDATIONS_EMPLOYEE_NUMBER_COL, validation.getEmployeeNumber());
        values.put(VALIDATIONS_SENT_STATE_COL, validation.getSentState().ordinal());
        long newRowId = db.insert(VALIDATIONS_TABLE_NAME, null, values);
        db.close();

        return newRowId;
    }

    public Employee[] getEmployees() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + EMPLOYEES_TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        Employee[] employees = new Employee[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            String number = cursor.getString(cursor.getColumnIndex(EMPLOYEES_ID_COL));
            String name = cursor.getString(cursor.getColumnIndex(EMPLOYEES_NAME_COL));
            int level = cursor.getInt(cursor.getColumnIndex(EMPLOYEES_LEVEL_COL));
            employees[i] = new Employee(number, name, level);
            i++;
        }
        cursor.close();
        db.close();
        return employees;
    }

    public Building[] getBuildings() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + BUILDINGS_TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        Building[] buildings = new Building[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(BUILDINGS_ID_COL));
            String name = cursor.getString(cursor.getColumnIndex(BUILDINGS_NAME_COL));
            String number = cursor.getString(cursor.getColumnIndex(BUILDINGS_NUMBER_COL));
            buildings[i] = new Building(id, name, number);
            i++;
        }
        cursor.close();
        db.close();
        return buildings;
    }

    public Asset[] getAssets() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + ASSETS_TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        Asset[] assets = new Asset[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            String number = cursor.getString(cursor.getColumnIndex(ASSETS_NUMBER_COL));
            String description = cursor.getString(cursor.getColumnIndex(ASSETS_DESCRIPTION_COL));
            String buildingName = cursor.getString(cursor.getColumnIndex(ASSETS_BUILDING_NAME_COL));
            String buildingId = cursor.getString(cursor.getColumnIndex(ASSETS_BUILDING_ID_COL));
            assets[i] = new Asset(number, description, buildingName, buildingId);
            i++;
        }
        cursor.close();
        db.close();
        return assets;
    }

    public void addEmployees(@NotNull Employee[] employees) {
        for (Employee employee : employees) {
            addEmployee(employee);
        }
    }

    public void addBuildings(@NotNull Building[] buildings) {
        for (Building building : buildings) {
            addBuilding(building);
        }
    }

    public void addAssets(@NotNull Asset[] assets) {
        for (Asset asset : assets) {
            addAsset(asset);
        }
    }

    public Asset getAssetByNumber(String number) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + ASSETS_TABLE_NAME + " WHERE " + ASSETS_NUMBER_COL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{number});
        Asset asset = null;
        if (cursor.moveToFirst()) {
            String description = cursor.getString(cursor.getColumnIndex(ASSETS_DESCRIPTION_COL));
            String buildingName = cursor.getString(cursor.getColumnIndex(ASSETS_BUILDING_NAME_COL));
            String buildingId = cursor.getString(cursor.getColumnIndex(ASSETS_BUILDING_ID_COL));
            asset = new Asset(number, description, buildingName, buildingId);
        }
        cursor.close();
        db.close();
        return asset;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS employees");
        db.execSQL("DROP TABLE IF EXISTS buildings");
        db.execSQL("DROP TABLE IF EXISTS assets");

        onCreate(db);
    }
}
