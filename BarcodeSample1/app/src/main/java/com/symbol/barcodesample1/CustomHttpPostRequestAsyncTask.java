package com.symbol.barcodesample1;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class CustomHttpPostRequestAsyncTask extends AsyncTask<String, Void, String> {
    private JSONObject requestBody;
    private Function<String, Void> callbackFunction;
    private String urlEndpoint;
    private String strResponse;
    private int responseCode;

    public CustomHttpPostRequestAsyncTask(String urlEndpoint, JSONObject requestBody) {
        this.urlEndpoint = urlEndpoint;
        this.requestBody = requestBody;
    }

    public void setCallbackFunction(Function<String, Void> callback) {
        this.callbackFunction = callback;
    }

    @Override
    protected String doInBackground(String... strings) {
        OutputStream out = null;
        try {
            URL url = new URL(this.urlEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            out = new BufferedOutputStream(conn.getOutputStream());

            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8)
            );

            writer.write(
                this.requestBody.toString()
            );

            writer.flush();
            writer.close();

            out.close();

            conn.connect();

            this.responseCode = conn.getResponseCode();
            this.strResponse = conn.getResponseMessage();

            return String.valueOf(this.responseCode);
        } catch (Exception e) {
            this.strResponse = e.getMessage();
            this.callbackFunction.apply(this.strResponse);

            System.out.println(e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (this.callbackFunction == null) {
            return;
        }

        try {
            this.callbackFunction.apply(String.valueOf(this.responseCode));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
