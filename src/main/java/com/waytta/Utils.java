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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.security.MasterToSlaveCallable;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import jenkins.model.Jenkins;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import hudson.util.FormValidation;

public class Utils {
    private static final String RETCODE_FIELD_NAME = "retcode";

    public static String getToken(Launcher launcher, String servername, JSONObject auth) throws InterruptedException, IOException {
        String token = "";
        JSONObject httpResponse = launcher.getChannel().call(new saltAPI(servername + "/login", auth, null));
        try {
            JSONArray returnArray = httpResponse.getJSONArray("return");
            for (Object o : returnArray) {
                JSONObject line = (JSONObject) o;
                // This token will be used for all subsequent connections
                token = line.getString("token");
            }
        } catch (Exception e) {
            token = "Auth Error: " + e + "\n\n" + httpResponse.toString(2).split("\\\\n")[0];
            return token;
        }
        return token;
    }

    // replaces $string with value of env($string). Used in conjunction with
    // parameterized builds
    public static String paramorize(Run build, TaskListener listener, String paramer)
            throws IOException, InterruptedException {
        Pattern pattern = Pattern.compile("\\{\\{\\w+\\}\\}");
        Matcher matcher = pattern.matcher(paramer);
        while (matcher.find()) {
            // listener.getLogger().println("FOUND: "+matcher.group());
            EnvVars envVars;
            envVars = build.getEnvironment(listener);
            // remove leading {{
            String replacementVar = matcher.group().substring(2);
            // remove trailing }}
            replacementVar = replacementVar.substring(0, replacementVar.length() - 2);
            // using proper env var name, perform a lookup and save value
            if (envVars.get(replacementVar) == null) {
                listener.fatalError("Could not find environment variable");
            }
            replacementVar = envVars.get(replacementVar, "");
            paramer = paramer.replace(matcher.group(), replacementVar);
        }
        return paramer;
    }

    public static boolean validateFunctionCall(JSONArray returnArray) {
        boolean result = true;

        // Salt's /hook url returns non standard response. Assume this response
        // is valid
        JSONArray successHook = JSONArray.fromObject("[{\"Success\": True}]");
        if (returnArray.equals(successHook)) {
            return true;
        }

        try {
            if (returnArray.get(0).toString().contains("TypeError")) {
                return false;
            } else if (returnArray.getJSONObject(0).has("Error")) {
                // detect [{"Error": ...
                return false;
            }
        } catch (Exception e) {
        }

        try {
            if (returnArray.getJSONArray(0).isArray()) {
                // detect runner manage.present result [["minion1", "minion2"...
                return true;
            }
        } catch (Exception e) {
        }

        for (Object o : returnArray) {
            if (o instanceof Boolean) {
                result = (Boolean) o;
            } else if (o instanceof String) {
                result = false;
            } else {
                JSONObject possibleMinion = JSONObject.fromObject(o);
                for (Object name : possibleMinion.names()) {
                    Object field = possibleMinion.get(name.toString());
                    
                    // Match test failedJSON/commandNotAvailable.json 
                    Pattern notFoundPattern = Pattern.compile(".*/bin/sh: 1: \\w+: not found.*");
                    Matcher matcher = notFoundPattern.matcher(field.toString());
                    if (matcher.matches()) {
                        return false;
                    }

                    // Match test failedJSON/functionNotAvailable.json
                    if (field.toString().contains(" is not available.")) {
                        return false;
                    }
                    
                    // detect errors like "return":[{"minionname":["Rendering SLS...
                    // failed"]}]
                    if (field instanceof JSONArray) {
                        return false;
                    } 
                }

                // test if normal minion results are a JSONArray which indicates
                // failure
                // detect errors like
                // "return":[{"data":{"minionname":["Rendering SLS...
                // failed"]}}]
                if (possibleMinion.has("data")) {
                    JSONObject minionData = possibleMinion.optJSONObject("data");
                    if (minionData != null) {
                        for (Object name : minionData.names()) {
                            Object field = minionData.get(name.toString());
                            if (field instanceof JSONArray) {
                                result = false;

                                if (!result) {
                                    return result;
                                }
                            }
                        }
                    }
                }

                // iterate through subkeys and values to detect failures
                result = validateInnerJsonObject((JSONObject) o);
                if (!result) {
                    break;
                }
            }
        }

        return result;
    }

    private static boolean validateInnerJsonObject(JSONObject minion) {
        boolean result = true;

        for (Object name : minion.names()) {
            Object field = minion.get(name.toString());

            if (field instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) field;

                // test if cmd.run return is non zero
                if (jsonObject.has(RETCODE_FIELD_NAME)) {
                    result = jsonObject.getInt(RETCODE_FIELD_NAME) == 0;

                    if (!result) {
                        break;
                    }
                }

                // test if result is false
                if (jsonObject.has("result")) {
                    // detect where test=True and results key is "null"
                    // See test testHighStateChangesTest
                    if (jsonObject.get("result").equals("null")) {
                        // deteced null result, skipping
                        break;
                    }
                    result = jsonObject.getBoolean("result");

                    if (!result) {
                        break;
                    }
                }

                result = validateInnerJsonObject(jsonObject);

                if (!result) {
                    break;
                }
            }
        }
        return result;
    }

    public static FormValidation validateFormStringField(String value, String lackOfFieldMessage,
            String fieldToShortMessage) {
        if (value.length() == 0) {
            return FormValidation.error(lackOfFieldMessage);
        }

        if (value.equals("*")) {
            return FormValidation.ok();
        }

        if (value.length() < 3) {
            return FormValidation.warning(fieldToShortMessage);
        }

        return FormValidation.ok();
    }

    public static FormValidation validatePillar(String value) {
        if (value.length() > 0) {
            // Check to see if paramorized. Ex: {{variable}}
            // This cannot be evaluated until build, so trust that all is well
            Pattern pattern = Pattern.compile("\\{\\{\\w+\\}\\}");
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches()) {
                return FormValidation.ok();
            }
            try {
                // If value was already a jsonobject, treat it as such
                JSONSerializer.toJSON(value);
                return FormValidation.ok();
            } catch (JSONException e) {
                // Otherwise it must have been a string
                return FormValidation.error("Requires data in JSON format");
            }
        }
        return FormValidation.ok();
    }

    public static JSONObject createAuthArray(StandardUsernamePasswordCredentials credential, String authtype) {
        JSONObject auth = new JSONObject();
        auth.put("username", credential.getUsername());
        auth.put("password", credential.getPassword().getPlainText());
        auth.put("eauth", authtype);

        return auth;
    }
}
