package com.symbol.barcodesample1.controllers;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.symbol.barcodesample1.Employee;
import com.symbol.barcodesample1.MainActivity;
import com.symbol.barcodesample1.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class AuthController {
    private final Activity activity;
    private final View view;

    private String username;
    private String password;
    private Employee currentEmployee;

    public AuthController(Activity activity, View view) {
        this.activity = activity;
        this.view = view;
    }

    private void onLoginFailed(IOException e) {
        TextView textViewLoginStatus = (TextView) view.findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(e.getMessage());
        textViewLoginStatus.setVisibility(View.VISIBLE);

        this.password = "";
        EditText passwordEditText = (EditText) view.findViewById(R.id.password);
        passwordEditText.setText(this.password);
    }

    private void getUserName() {
        EditText usernameEditText = (EditText) view.findViewById(R.id.userName);
        this.username = usernameEditText.getText().toString();
    }

    private void getPassword() {
        EditText passwordEditText = (EditText) view.findViewById(R.id.password);
        this.password = passwordEditText.getText().toString();
    }

    private void localLogin() {
        TextView textViewLoginStatus = (TextView) view.findViewById(R.id.loginProgress);

        boolean isEmployeeSavedOnLocal = dbHandler.isEmployeeInDB(this.username);
        if (isEmployeeSavedOnLocal) {
            Log.d("login", "employee is saved on local");

            Employee employee = dbHandler.getEmployeeByNumber(this.username);
            Log.d("login", "local employee: " + employee.getNumber() + " " + employee.getName() + " " + employee.getLevel());

            String password = dbHandler.getEmployeePassword(employee);
            Log.d("login", "local password: " + password);

            boolean hasPassword = password != null && !password.isEmpty();
            if (hasPassword) {

                if (password.equals(this.password)) {
                    Log.d("login", "local password is correct");
                    currentEmployee = employee;

                    loadSpinnerData();

                    Toast.makeText(
                            activity,
                            "Bienvenido " + currentEmployee.getName(),
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    Log.d("login", "local password is incorrect");

                    textViewLoginStatus.setText(R.string.login_error_text);
                    textViewLoginStatus.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void onLoginSuccess(JSONObject responseBody) throws JSONException {
        Log.d("login", "login success");
        Log.d("login", responseBody.toString());

        JSONObject jsonEmployee = responseBody.getJSONObject("usuario");

        currentEmployee = new Employee(
                jsonEmployee.getString("numeroEmpleado"),
                jsonEmployee.getString("nombre"),
                jsonEmployee.getInt("nivel")
        );
        dbHandler.addEmployee(currentEmployee);
        dbHandler.updateEmployeePassword(currentEmployee, this.password);

        Log.d("login", "employee saved on local");

        loadSpinnerData();

        Toast.makeText(
                activity,
                "Hola " + currentEmployee.getName() + "!",
                Toast.LENGTH_LONG
        ).show();
    }

    private void onLoginFailed() {
        TextView textViewLoginStatus = (TextView) view.findViewById(R.id.loginProgress);
        textViewLoginStatus.setText(R.string.login_error_text);
        textViewLoginStatus.setVisibility(View.VISIBLE);
    }

    private void onLogOut() {
        currentEmployee = null;
        username = "";
        password = "";

        Toast.makeText(
                activity,
                "Adiós " + currentEmployee.getName(),
                Toast.LENGTH_LONG
        ).show();
    }
}
