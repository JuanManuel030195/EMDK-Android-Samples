package com.symbol.barcodesample1.controllers;

import android.util.Log;

import com.symbol.barcodesample1.AssetPerValidation;
import com.symbol.barcodesample1.LocalValidation;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.function.Function;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestApiClientController {
    private static final MediaType jsonMediaType = MediaType.parse("application/json; charset=utf-8");
    private static final String baseUrl = "http://cda.ceaqueretaro.gob.mx/";

    private static String username = "";
    private static String password = "";

    public static void setUsername(String username) {
        RestApiClientController.username = username;
    }

    public static void setPassword(String password) {
        RestApiClientController.password = password;
    }

    public static void getAssets(
        Function<IOException, Void> onFail,
        Function<JSONObject, Void> onSuccess
    ) {
        String endPoint = "index.php?r=auth%2Fbackup";
        sendPostRequestToEndpoint(endPoint, onFail, onSuccess);
    }

    public static void getEmployees(
        Function<IOException, Void> onFail,
        Function<JSONObject, Void> onSuccess
    ) {
        String endPoint = "index.php?r=auth%2Fempleados";
        sendPostRequestToEndpoint(endPoint, onFail, onSuccess);
    }

    public static void getBuildings(
        Function<IOException, Void> onFail,
        Function<JSONObject, Void> onSuccess
    ) {
        String endPoint = "index.php?r=auth%2Fedificios";
        sendPostRequestToEndpoint(endPoint, onFail, onSuccess);
    }

    public static void sendValidation(
        JSONObject requestBody,
        Function<IOException, Void> onFail,
        Function<JSONObject, Void> onSuccess
    ) {
        String endPoint = "index.php?r=auth%2Fconfronta";
        sendPostRequestToEndpoint(endPoint, requestBody, onFail, onSuccess);
    }

    public static void logOut(
        Function<IOException, Void> onFail,
        Function<JSONObject, Void> onSuccess
    ) {
        String endPoint = "index.php?r=auth%2Flogout";
        sendPostRequestToEndpoint(endPoint, onFail, onSuccess);
    }

    public static void login(
        Function<IOException, Void> onFail,
        Function<JSONObject, Void> onSuccess
    ) {
        String endPoint = "index.php?r=auth%2Flogin";
        sendPostRequestToEndpoint(endPoint, onFail, onSuccess);
    }

    private static void sendPostRequestToEndpoint(
        String endPoint,
        Function<IOException, Void> onFail,
        Function<JSONObject, Void> onSuccess
    ) {
        JSONObject requestBody = getRequestBody();
        if (requestBody == null) {
            return;
        }

        try {
            postRequest(endPoint, requestBody, onFail, onSuccess);
        } catch (IOException e) {
            onFail.apply(e);
        }
    }

    private static void sendPostRequestToEndpoint(
        String endPoint,
        JSONObject requestBody,
        Function<IOException, Void> onFail,
        Function<JSONObject, Void> onSuccess
    ) {
        try {
            postRequest(endPoint, requestBody, onFail, onSuccess);
        } catch (IOException e) {
            onFail.apply(e);
        }
    }

    private static JSONObject getRequestBody() {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("numeroEmpleado", username);
            requestBody.put("password", password);
        } catch (JSONException e) {
            Log.e("getRequestBody", e.getMessage());
            return null;
        }
        return requestBody;
    }


    private static void postRequest(
            String endPoint,
            @NotNull JSONObject requestBody,
            Function<IOException, Void> onFail,
            Function<JSONObject, Void> onSuccess
    ) throws IOException {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(jsonMediaType, requestBody.toString());
        String url = baseUrl + endPoint;
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
                onFail.apply(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    return;
                }

                try {
                    final String responseBodyString = response.body().string();
                    final JSONObject responseBody = new JSONObject(responseBodyString);
                    if (responseBody.has("success") && responseBody.getBoolean("success")) {
                        onSuccess.apply(responseBody);
                    } else {
                        onFail.apply(new IOException(responseBodyString));
                    }
                } catch (JSONException e) {
                    Log.e("JSONException", e.getMessage());
                }
            }
        });
    }
}
