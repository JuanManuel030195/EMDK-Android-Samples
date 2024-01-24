package com.symbol.barcodesample1;

import org.json.JSONObject;

import java.util.function.Function;

public class RestApiController {
    private final String urlBase = "http://cda.ceaqueretaro.gob.mx";

    public void post(String endpoint, JSONObject requestBody, Function<String, Void> callbackFunction) {
        String urlString = urlBase + endpoint;
        CustomHttpPostRequestAsyncTask task = new CustomHttpPostRequestAsyncTask(urlString, requestBody);
        task.setCallbackFunction(callbackFunction);
        task.execute();
    }
}
