package com.waytta;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;


class saltAPI extends MasterToSlaveCallable<JSONObject, IOException> {
    private static final long serialVersionUID = 1L;
    private String targetURL;
    private JSONObject urlParams;
    private String auth;

    public saltAPI(String targetURL, JSONObject urlParams, String auth) {
        this.targetURL = targetURL;
        this.urlParams = urlParams;
        this.auth = auth;
    }

    @Override
    public JSONObject call() throws IOException {
        HttpURLConnection connection = null;
        JSONObject responseJSON = new JSONObject();

        try {
            // Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setUseCaches(false);
            if ((urlParams != null && !urlParams.isEmpty()) || targetURL.contains("/hook") ) {
                // We have stuff to send, so do an HTTP POST not GET
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

            }
            connection.setConnectTimeout(5000); // set timeout to 5 seconds
            if (auth != null && !auth.isEmpty()) {
                connection.setRequestProperty("X-Auth-Token", auth);
            }

            // Send request
            if ((urlParams != null && !urlParams.isEmpty()) || targetURL.contains("/hook")) {
                // only necessary if we have stuff to send
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(urlParams.toString());
                wr.flush();
                wr.close();
            }

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            String responseText = response.toString();
            if (responseText.contains("java.io.IOException")
                    || responseText.contains("java.net.SocketTimeoutException")) {
                responseJSON.put("Error", responseText);
                return responseJSON;
            }
            try {
                // Server response should be json so this should work
                responseJSON = (JSONObject) JSONSerializer.toJSON(responseText);
                return responseJSON;
            } catch (Exception e) {
                responseJSON.put("Error", e);
                return responseJSON;
            }
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            responseJSON.put("Error", errors.toString());
            return responseJSON;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}