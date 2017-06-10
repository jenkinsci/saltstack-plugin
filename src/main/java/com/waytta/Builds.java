package com.waytta;

import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.Iterator;

import hudson.Launcher;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class Builds {
    public static void addArgumentsToSaltFunction(String myarguments, JSONObject saltFunc) {
        if (myarguments.length() > 0) {
            JSONObject fullKwJSON = new JSONObject();

            // spit on space separated not inside of single and double quotes
            String[] argItems = myarguments.split("\\s+(?=(?:[^'\"]|'[^']*'|\"[^\"]*\")*$)");

            for (String arg : argItems) {
                // remove spaces at beginning or end
                arg = arg.replaceAll("^\\s+|\\s+$", "");
                // if string wrapped in quotes, remove them since adding to list
                // re-quotes
                arg = arg.replaceAll("(^')|(^\")|('$)|(\"$)", "");
                if (arg.contains("=")) {
                    String[] kwString = arg.split("=");
                    if (kwString.length > 2) {
                        // kwarg contained more than one =. Let's put the string
                        // back together
                        String kwFull = "";
                        for (String kwItem : kwString) {
                            // Ignore the first item as it will remain the key
                            if (kwItem.equals(kwString[0])) {
                                continue;
                            }
                            // add the second item
                            if (kwItem.equals(kwString[1])) {
                                // again remove any wrapping quotes
                                kwItem = kwItem.replaceAll("(^')|(^\")|('$)|(\"$)", "");
                                kwFull += kwItem;
                                continue;
                            }
                            // add all other items with an = to rejoin
                            kwFull += "=" + kwItem;
                        }
                        fullKwJSON.put(kwString[0], kwFull);
                    } else {
                        // again remove any wrapping quotes
                        kwString[1] = kwString[1].replaceAll("(^')|(^\")|('$)|(\"$)", "");
                        try {
                            // If value was already a jsonobject, treat it as such
                            JSON kwJSON = JSONSerializer.toJSON(kwString[1]);
                            // Rejoin key and json object
                            fullKwJSON.element(kwString[0], kwJSON);
                        } catch (JSONException e) {
                            // Otherwise it must have been a string
                            if (isInteger(kwString[1])) {
                                fullKwJSON.put(kwString[0], Integer.parseInt(kwString[1]));
                            } else {
                                fullKwJSON.put(kwString[0], kwString[1]);
                            }
                        }
                    }
                } else {
                    // Add any args to json message
                    if (isInteger(arg)) {
                        // if arg is int, add as int
                        saltFunc.accumulate("arg", Integer.parseInt(arg));
                    } else {
                        // otherwise assume string
                        saltFunc.accumulate("arg", arg);
                    }
                }
            }
            // Now that loops have completed, add kwarg object
            saltFunc.element("kwarg", fullKwJSON);
        }
    }

    private static boolean isInteger(String s) {
        boolean isInteger = false;
        try {
            Integer.parseInt(s);
            isInteger = true;
        }
        catch (NumberFormatException ex) {
            // s is not an integer
        }
        return isInteger;
    }

    public static int returnedMinions(JSONArray saltReturn) {
        int numMinionsDone = 0;
        JSONObject resultObject = new JSONObject();

        resultObject = saltReturn.getJSONObject(0).getJSONObject("Result");
        // Check Result for number of responses
        Iterator<?> rMinionKeys = resultObject.keys();
        while( rMinionKeys.hasNext() ) {
            String key = (String)rMinionKeys.next();
            if ( resultObject.get(key) instanceof JSONObject ) {
                numMinionsDone += 1;
            }
        }
        return numMinionsDone;
    }

    public static JSONArray returnData(JSONObject saltReturn, String netapi) {
        JSONArray returnArray = new JSONArray();

        if (netapi.contains("TornadoServer")) {
            // Tornado is keyed with return
            returnArray = saltReturn.getJSONArray("return");
        } else {
            // cherrypy is keyed with info
            returnArray = saltReturn.getJSONArray("info");
        }
        return returnArray;
    }

    public static String getBlockingBuildJid(Launcher launcher, String myservername,
            String token, JSONObject saltFunc, TaskListener listener)
                    throws IOException, InterruptedException, SaltException {
        String jid = null;
        // Send request to /minion url. This will give back a jid which we
        // will need to poll and lookup for completion
        JSONObject httpResponse = launcher.getChannel().call(new HttpCallable(myservername + "/minions", saltFunc, token));
        JSONArray returnArray = httpResponse.getJSONArray("return");
        for (Object o : returnArray) {
            JSONObject line = (JSONObject) o;
            jid = line.getString("jid");
        }
        // Print out success
        listener.getLogger().println("Running jid: " + jid);
        return jid;
    }

    public static JSONArray checkBlockingBuild(Launcher launcher, String myservername, String token,
            JSONObject saltFunc, TaskListener listener, int pollTime, int minionTimeout, String netapi, String jid)
                    throws IOException, InterruptedException, SaltException {
        // Request successfully sent. Now use jid to check if job complete
        int numMinions = 0;
        int numMinionsDone = 0;
        JSONArray minionsArray = new JSONArray();
        JSONObject resultObject = new JSONObject();
        JSONArray httpArray = new JSONArray();
        JSONArray returnArray = new JSONArray();
        JSONObject httpResponse = new JSONObject();
        httpResponse = launcher.getChannel().call(new HttpCallable(myservername + "/jobs/" + jid, null, token));
        httpArray = returnData(httpResponse, netapi);
        for (Object o : httpArray) {
            JSONObject line = (JSONObject) o;
            minionsArray = line.getJSONArray("Minions");
            resultObject = line.getJSONObject("Result");
        }

        // Check Minions array for number of targets minions
        numMinions = minionsArray.size();
        listener.getLogger().println("Waiting for " + numMinions + " minions");

        numMinionsDone = returnedMinions(httpArray);
        listener.getLogger().println(numMinionsDone + " minions are done");

        // Now that we know how many minions have responded, and how many we
        // are waiting on. Let's see more have finished
        if (numMinionsDone < numMinions) {
            // Don't print annoying messages unless we really are waiting for
            // more minions to return
            listener.getLogger().println(
                    "Will check status every " + pollTime + " seconds...");
        }
        while (numMinionsDone < numMinions) {
            try {
                Thread.sleep(pollTime * 1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                // Allow user to cancel job in jenkins interface
                throw new InterruptedException();
            }
            httpResponse = launcher.getChannel().call(new HttpCallable(myservername + "/jobs/" + jid, null, token));
            httpArray = returnData(httpResponse, netapi);
            numMinionsDone = returnedMinions(httpArray);

            // if minionTimeout is negative, still walkthrough the timeout process, but do not fail build
            boolean timeoutFail = true;
            if ( minionTimeout < 0) {
                timeoutFail = false;
                minionTimeout *= -1;
            }

            if (numMinionsDone > 0 && numMinionsDone < numMinions) {
                // Some minions returned, but not all
                // Give them minionTimeout to all return or fail build
                try {
                    listener.getLogger().println(
                            "Some minions returned. Waiting " + minionTimeout + " seconds");
                    int numberChecks = minionTimeout / pollTime;
                    int numberChecksRemain = minionTimeout % pollTime;
                    // Check every pollTime seconds until minionTimeout.
                    for (int i=0; i<numberChecks; i++) {
                        httpResponse = launcher.getChannel().call(new HttpCallable(myservername + "/jobs/" + jid, null, token));
                        httpArray = returnData(httpResponse, netapi);
                        numMinionsDone = returnedMinions(httpArray);
                        if (numMinionsDone >= numMinions) {
                            returnArray.clear();
                            returnArray.add(httpArray);
                            return returnArray;
                        }
                        Thread.sleep(pollTime * 1000L);
                    }
                    if (numberChecksRemain > 0) {
                        // If on last iteration of loop, sleep remainder between
                        // pollTime and minionTimeout to fully reach minionTimeout
                        Thread.sleep(numberChecksRemain * 1000L);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    // Allow user to cancel job in jenkins interface
                    throw new InterruptedException();
                }
                httpResponse = launcher.getChannel().call(new HttpCallable(myservername + "/jobs/" + jid, null, token));
                httpArray = returnData(httpResponse, netapi);
                numMinionsDone = returnedMinions(httpArray);
                if (numMinionsDone < numMinions) {
                    JSONArray respondedMinions = new JSONArray();
                    Iterator<?> rMinionKeys = httpArray.getJSONObject(0).getJSONObject("Result").keys();
                    while( rMinionKeys.hasNext() ) {
                        String key = (String)rMinionKeys.next();
                        respondedMinions.add(key);
                    }
                    for (int i = 0; i < respondedMinions.size(); ++i) {
                        minionsArray.discard(respondedMinions.get(i));
                    }
                    listener.error(
                            "Minions timed out:\n" + minionsArray.toString() + "\n\n");
                    if (timeoutFail) {
                        throw new SaltException(httpArray.getJSONObject(0).getJSONObject("Result").toString());
                    }
                    returnArray.clear();
                    returnArray.add(httpArray.getJSONObject(0).getJSONObject("Result"));
                    return returnArray;
                }
            }
        }
        returnArray.clear();
        returnArray.add(httpArray.getJSONObject(0).getJSONObject("Result"));
        return returnArray;
    }
}