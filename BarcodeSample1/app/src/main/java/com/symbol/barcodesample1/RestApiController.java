package com.symbol.barcodesample1;

import org.json.JSONObject;

import java.util.concurrent.Callable;

public class RestApiController {
    private final String urlBase = "http://cda.ceaqueretaro.gob.mx";

    public void post(String endpoint, JSONObject requestBody, Callable<Void> callback) {
        String urlString = urlBase + endpoint;
        CustomHttpPostRequestAsyncTask task = new CustomHttpPostRequestAsyncTask(urlString, requestBody);
        task.setCallback(callback);
        task.execute();
    }
}
