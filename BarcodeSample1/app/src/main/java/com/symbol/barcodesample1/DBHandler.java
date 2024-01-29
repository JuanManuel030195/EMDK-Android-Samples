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
    private static final String EMPLOYEES_ID_COL = "numeroEmpleado"; // primary key (string)
    private static final String EMPLOYEES_NAME_COL = "nombre"; // string
    private static final String EMPLOYEES_LEVEL_COL = "nivel"; // integer

    private static final String BUILDINGS_TABLE_NAME = "edificios";
    private static final String BUILDINGS_ID_COL = "idEdificio"; // primary key (integer)
    private static final String BUILDINGS_NAME_COL = "nombre"; // string
    private static final String BUILDINGS_NUMBER_COL = "numero"; // string

    private static final String ASSETS_TABLE_NAME = "activos";
    private static final String ASSETS_NUMBER_COL = "numeroSAP"; // primary key (string)
    private static final String ASSETS_EMPLOYEE_ID_COL = "numeroEmpleado"; // foreign key (string), target: EMPLOYEES_ID_COL from EMPLOYEES_TABLE_NAME
    private static final String ASSETS_DESCRIPTION_COL = "descripcion"; // string
    private static final String ASSETS_BUILDING_NAME_COL = "edificio"; // string
    private static final String ASSETS_BUILDING_ID_COL = "idEdificio"; // foreign key (integer), target: BUILDINGS_ID_COL from BUILDINGS_TABLE_NAME

    private static final String VALIDATIONS_TABLE_NAME = "validaciones";
    private static final String VALIDATIONS_ID_COL = "idValidacion"; // primary key (integer)
    private static final String VALIDATIONS_DATE_COL = "date"; // datetime
    private static final String VALIDATIONS_EMPLOYEE_NUMBER_COL = "numeroEmpleado"; // foreign key (string), target: EMPLOYEES_ID_COL from EMPLOYEES_TABLE_NAME
    private static final String VALIDATIONS_BUILDING_ID_COL = "idEdificio"; // foreign key (integer), target: BUILDINGS_ID_COL from BUILDINGS_TABLE_NAME
    private static final String VALIDATIONS_SENT_STATE_COL = "sentState"; // boolean

    private static final String ASSETS_PER_VALIDATION_TABLE_NAME = "activosPorValidacion";
    private static final String ASSETS_PER_VALIDATION_VALIDATION_ID_COL = "idValidacion"; // foreign key (integer), target: VALIDATIONS_ID_COL from VALIDATIONS_TABLE_NAME
    private static final String ASSETS_PER_VALIDATION_ASSET_NUMBER_COL = "numeroSAP"; // foreign key (string), target: ASSETS_NUMBER_COL from ASSETS_TABLE_NAME
    private static final String ASSETS_PER_VALIDATION_ASSET_SCANNED_COL = "escaneado"; // boolean
    private static final String ASSETS_PER_VALIDATION_ASSET_STATUS_COL = "estado"; // integer

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
                ASSETS_EMPLOYEE_ID_COL + " TEXT, " +
                ASSETS_DESCRIPTION_COL + " TEXT, " +
                ASSETS_BUILDING_NAME_COL + " TEXT, " +
                ASSETS_BUILDING_ID_COL + " INTEGER)";
        db.execSQL(createAssetsTable);

        String createValidationsTable = "CREATE TABLE " + VALIDATIONS_TABLE_NAME + " (" +
                VALIDATIONS_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                VALIDATIONS_DATE_COL + " DATETIME, " +
                VALIDATIONS_EMPLOYEE_NUMBER_COL + " TEXT, " +
                VALIDATIONS_BUILDING_ID_COL + " INTEGER, " +
                VALIDATIONS_SENT_STATE_COL + " INTEGER)";
        db.execSQL(createValidationsTable);

        String createAssetsPerValidationTable = "CREATE TABLE " + ASSETS_PER_VALIDATION_TABLE_NAME + " (" +
                ASSETS_PER_VALIDATION_VALIDATION_ID_COL + " INTEGER, " +
                ASSETS_PER_VALIDATION_ASSET_NUMBER_COL + " TEXT, " +
                ASSETS_PER_VALIDATION_ASSET_SCANNED_COL + " INTEGER, " +
                ASSETS_PER_VALIDATION_ASSET_STATUS_COL + " INTEGER)";
        db.execSQL(createAssetsPerValidationTable);
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
        values.put(ASSETS_EMPLOYEE_ID_COL, asset.getEmployeeNumber());
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
        values.put(VALIDATIONS_BUILDING_ID_COL, validation.getId());
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
            String employeeNumber = cursor.getString(cursor.getColumnIndex(ASSETS_EMPLOYEE_ID_COL));
            String description = cursor.getString(cursor.getColumnIndex(ASSETS_DESCRIPTION_COL));
            String buildingName = cursor.getString(cursor.getColumnIndex(ASSETS_BUILDING_NAME_COL));
            int buildingId = cursor.getInt(cursor.getColumnIndex(ASSETS_BUILDING_ID_COL));
            assets[i] = new Asset(number, description, buildingName, buildingId, employeeNumber);
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

    public void addAssetsPerValidation(@NotNull LocalValidation localValidation) {
        Asset[] assets = getAssetsByValidation(localValidation);
        SQLiteDatabase db = getWritableDatabase();
        for (Asset asset : assets) {
            ContentValues values = new ContentValues();
            values.put(ASSETS_PER_VALIDATION_VALIDATION_ID_COL, localValidation.getId());
            values.put(ASSETS_PER_VALIDATION_ASSET_NUMBER_COL, asset.getNumber());
            values.put(ASSETS_PER_VALIDATION_ASSET_SCANNED_COL, 0);
            values.put(ASSETS_PER_VALIDATION_ASSET_STATUS_COL, ValidationStatus.PENDING.ordinal());
            db.insert(ASSETS_PER_VALIDATION_TABLE_NAME, null, values);
        }
        db.close();
    }

    public void addAssetsPerValidation(
        @NotNull LocalValidation localValidation,
        @NotNull Asset[] assets)
    {
        SQLiteDatabase db = getWritableDatabase();
        for (Asset asset : assets) {
            ContentValues values = new ContentValues();
            values.put(ASSETS_PER_VALIDATION_VALIDATION_ID_COL, localValidation.getId());
            values.put(ASSETS_PER_VALIDATION_ASSET_NUMBER_COL, asset.getNumber());
            values.put(ASSETS_PER_VALIDATION_ASSET_SCANNED_COL, 0);
            values.put(ASSETS_PER_VALIDATION_ASSET_STATUS_COL, ValidationStatus.PENDING.ordinal());
            db.insert(ASSETS_PER_VALIDATION_TABLE_NAME, null, values);
        }
        db.close();
    }

    public Asset getAssetByNumber(String number) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + ASSETS_TABLE_NAME + " WHERE " + ASSETS_NUMBER_COL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{number});
        Asset asset = null;
        if (cursor.moveToFirst()) {
            String employeeNumber = cursor.getString(cursor.getColumnIndex(ASSETS_EMPLOYEE_ID_COL));
            String description = cursor.getString(cursor.getColumnIndex(ASSETS_DESCRIPTION_COL));
            String buildingName = cursor.getString(cursor.getColumnIndex(ASSETS_BUILDING_NAME_COL));
            int buildingId = cursor.getInt(cursor.getColumnIndex(ASSETS_BUILDING_ID_COL));
            asset = new Asset(number, description, buildingName, buildingId, employeeNumber);
        }
        cursor.close();
        db.close();
        return asset;
    }

    public Asset[] getAssetsByValidation(@NotNull LocalValidation validation) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + ASSETS_TABLE_NAME + " WHERE " + ASSETS_EMPLOYEE_ID_COL + " = ?" + " AND " + ASSETS_BUILDING_NAME_COL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{validation.getEmployeeNumber(), String.valueOf(validation.getBuilding().getName())});
        Asset[] assets = new Asset[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            String number = cursor.getString(cursor.getColumnIndex(ASSETS_NUMBER_COL));
            String employeeNumber = cursor.getString(cursor.getColumnIndex(ASSETS_EMPLOYEE_ID_COL));
            String description = cursor.getString(cursor.getColumnIndex(ASSETS_DESCRIPTION_COL));
            String buildingName = cursor.getString(cursor.getColumnIndex(ASSETS_BUILDING_NAME_COL));
            int buildingId = cursor.getInt(cursor.getColumnIndex(ASSETS_BUILDING_ID_COL));
            assets[i] = new Asset(number, description, buildingName, buildingId, employeeNumber);
            i++;
        }
        cursor.close();
        db.close();
        return assets;
    }


    public Employee getEmployeeByNumber(String number) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + EMPLOYEES_TABLE_NAME + " WHERE " + EMPLOYEES_ID_COL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{number});
        Employee employee = null;
        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex(EMPLOYEES_NAME_COL));
            int level = cursor.getInt(cursor.getColumnIndex(EMPLOYEES_LEVEL_COL));
            employee = new Employee(number, name, level);
        }
        cursor.close();
        db.close();
        return employee;
    }

    public Employee getEmployeeByName(String name) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + EMPLOYEES_TABLE_NAME + " WHERE " + EMPLOYEES_NAME_COL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{name});
        Employee employee = null;
        if (cursor.moveToFirst()) {
            String number = cursor.getString(cursor.getColumnIndex(EMPLOYEES_ID_COL));
            int level = cursor.getInt(cursor.getColumnIndex(EMPLOYEES_LEVEL_COL));
            employee = new Employee(number, name, level);
        }
        cursor.close();
        db.close();
        return employee;
    }

    public Building getBuildingById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + BUILDINGS_TABLE_NAME + " WHERE " + BUILDINGS_ID_COL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});
        Building building = null;
        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex(BUILDINGS_NAME_COL));
            String number = cursor.getString(cursor.getColumnIndex(BUILDINGS_NUMBER_COL));
            building = new Building(id, name, number);
        }
        cursor.close();
        db.close();
        return building;
    }

    public Building getBuildingByName(String name) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + BUILDINGS_TABLE_NAME + " WHERE " + BUILDINGS_NAME_COL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{name});
        Building building = null;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(BUILDINGS_ID_COL));
            String number = cursor.getString(cursor.getColumnIndex(BUILDINGS_NUMBER_COL));
            building = new Building(id, name, number);
        }
        cursor.close();
        db.close();
        return building;
    }

    public AssetPerValidation getAssetPerValidationById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + ASSETS_PER_VALIDATION_TABLE_NAME + " WHERE " + ASSETS_PER_VALIDATION_VALIDATION_ID_COL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});
        AssetPerValidation assetPerValidation = null;
        if (cursor.moveToFirst()) {
            String assetNumber = cursor.getString(cursor.getColumnIndex(ASSETS_PER_VALIDATION_ASSET_NUMBER_COL));
            boolean scanned = cursor.getInt(cursor.getColumnIndex(ASSETS_PER_VALIDATION_ASSET_SCANNED_COL)) == 1;
            ValidationStatus status = ValidationStatus.values()[cursor.getInt(cursor.getColumnIndex(ASSETS_PER_VALIDATION_ASSET_STATUS_COL))];
            assetPerValidation = new AssetPerValidation(id, assetNumber);
            assetPerValidation.setScanned(scanned);
            assetPerValidation.setStatus(getAssetByNumber(assetNumber), getEmployeeByNumber(assetNumber), getBuildingByName(assetNumber));
        }
        cursor.close();
        db.close();
        return assetPerValidation;
    }

    public void clearEmployees() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + EMPLOYEES_TABLE_NAME);
        db.close();
    }

    public void clearBuildings() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + BUILDINGS_TABLE_NAME);
        db.close();
    }

    public void clearAssets() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + ASSETS_TABLE_NAME);
        db.close();
    }

    public void clearValidations() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + VALIDATIONS_TABLE_NAME);
        db.close();
    }

    public void clearAssetsPerValidation() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + ASSETS_PER_VALIDATION_TABLE_NAME);
        db.close();
    }

    public long getTotalEmployees() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + EMPLOYEES_TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        long count = cursor.getLong(0);
        cursor.close();
        db.close();
        return count;
    }

    public long getTotalBuildings() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + BUILDINGS_TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        long count = cursor.getLong(0);
        cursor.close();
        db.close();
        return count;
    }

    public long getTotalAssets() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + ASSETS_TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        long count = cursor.getLong(0);
        cursor.close();
        db.close();
        return count;
    }

    public void updateAssetPerValidation(@NotNull AssetPerValidation assetPerValidation) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "UPDATE " + ASSETS_PER_VALIDATION_TABLE_NAME + " SET " + ASSETS_PER_VALIDATION_ASSET_SCANNED_COL + " = ?, " + ASSETS_PER_VALIDATION_ASSET_STATUS_COL + " = ? WHERE " + ASSETS_PER_VALIDATION_VALIDATION_ID_COL + " = ? AND " + ASSETS_PER_VALIDATION_ASSET_NUMBER_COL + " = ?";
        db.execSQL(query, new String[]{
            String.valueOf(assetPerValidation.isScanned() ? 1 : 0),
            String.valueOf(assetPerValidation.getStatus().ordinal()),
            String.valueOf(assetPerValidation.getValidationId()),
            assetPerValidation.getAssetNumber()
        });

        db.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS '" + EMPLOYEES_TABLE_NAME + "'");
        db.execSQL("DROP TABLE IF EXISTS '" + BUILDINGS_TABLE_NAME + "'");
        db.execSQL("DROP TABLE IF EXISTS '" + ASSETS_TABLE_NAME + "'");
        db.execSQL("DROP TABLE IF EXISTS '" + VALIDATIONS_TABLE_NAME + "'");
        db.execSQL("DROP TABLE IF EXISTS '" + ASSETS_PER_VALIDATION_TABLE_NAME + "'");

        onCreate(db);
    }
}
