package com.waytta;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.security.MasterToSlaveCallable;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;


class HttpCallable extends MasterToSlaveCallable<String, IOException> {
    private static final long serialVersionUID = 1L;
    private String targetURL;
    private String urlParamsS;
    private String auth;

    public HttpCallable(String targetURL, JSONObject urlParams, String auth) {
        this.targetURL = targetURL;
        this.urlParamsS = urlParams.toString();
        this.auth = auth;
    }

    @Override
    public String call() throws IOException {
        final Logger LOGGER = Logger.getLogger("com.waytta.saltstack");

        JSONObject urlParams = (JSONObject) JSONSerializer.toJSON(urlParamsS);

        HttpURLConnection connection = null;
        JSONObject responseJSON = new JSONObject();

        try {
            // Retry http connection on timeout
            int RETRYCOUNT = 3;
            int currentCount = 0;
            int responseCode = -1;
            while (currentCount++ < RETRYCOUNT) {
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
                    connection.setConnectTimeout(30000); // set timeout to 30 seconds
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

                    responseCode = connection.getResponseCode();

                    // Retry on request timeout
                    if (responseCode == 408) {
                        throw new SocketTimeoutException("408 Response");
                    } else if (responseCode == 500) {
                        throw new SocketTimeoutException("500 Response");
                    }

                    // http call successful. Exit retry loop.
                    break;
                } catch (SocketTimeoutException e) {
                    if (currentCount < RETRYCOUNT) {
                        LOGGER.log(Level.FINE, e.getMessage() + " encountered " + currentCount + " times, retrying.");
                        try {
                            Thread.sleep(3 * 1000L);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        LOGGER.log(Level.FINE, e.getMessage() + " encountered " + currentCount + " times. Failing");
                    }
                }
            }

            // Check http response code and fail out on error
            if (responseCode < 200 || responseCode > 299) {
                String responseError = "";

                try {
                    InputStream err = connection.getErrorStream();
                    if (err != null) {
                        responseError += "HTTP ERROR: \n" + read(err) + "\n\n";
                        err.close();
                    }
                } catch(Exception e){
                }

                try {
                    InputStream str = connection.getInputStream();
                    if (str != null) {
                        responseError += "HTTP Response: \n" + read(str);
                        str.close();
                    }
                } catch(Exception e) {
                }


                throw new IOException("Bad ResponseCode: " +
                        connection.getResponseCode() + " " +
                        connection.getResponseMessage() + "\n" +
                        responseError);
            }

            // Get Response
            InputStream is = connection.getInputStream();
            String responseText = read(is);

            // Server response should be json so this should work
            responseJSON = (JSONObject) JSONSerializer.toJSON(responseText);
            // pass along server header during login
            if (auth == null || auth.isEmpty()) {
                String serverHeader = "unknown";
                serverHeader = connection.getHeaderField("Server");
                responseJSON.element("server", serverHeader);
            }
            return responseJSON.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String read(InputStream stream) throws IOException {
        BufferedReader rdr = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        StringBuffer response = new StringBuffer();
        while ((line = rdr.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        rdr.close();

        return response.toString();
    }
}