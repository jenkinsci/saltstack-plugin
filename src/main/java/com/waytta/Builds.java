package com.waytta;

import hudson.model.Run;
import hudson.model.Result;
import hudson.model.TaskListener;

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
                        String kwFull = new String();
                        for (String kwItem : kwString) {
                            // Ignore the first item as it will remain the key
                            if (kwItem == kwString[0]) {
                                continue;
                            }
                            // add the second item
                            if (kwItem == kwString[1]) {
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
                            fullKwJSON.put(kwString[0], kwString[1]);
        	            }
                    }
                } else {
                	// Add any args to json message
                	saltFunc.accumulate("arg", arg);
                }
            }
            // Now that loops have completed, add kwarg object
            saltFunc.element("kwarg", fullKwJSON);
        }
    }
    

    public static JSONArray runBlockingBuild(Run build, JSONArray returnArray, String myservername, 
    		String token, JSONObject saltFunc, TaskListener listener, int pollTime, int minionTimeout) throws InterruptedException {
    	JSONObject httpResponse = new JSONObject();
    	String jid = new String();
    	// Send request to /minion url. This will give back a jid which we
    	// will need to poll and lookup for completion
    	httpResponse = Utils.getJSON(myservername + "/minions", saltFunc, token);
    	try {
    		returnArray = httpResponse.getJSONArray("return");
    		for (Object o : returnArray) {
    			JSONObject line = (JSONObject) o;
    			jid = line.getString("jid");
    		}
    		// Print out success
    		listener.getLogger().println("Running jid: " + jid);
    	} catch (Exception e) {
    		listener.error(httpResponse.toString(2));
    		build.setResult(Result.FAILURE);
    	}

    	// Request successfully sent. Now use jid to check if job complete
    	int numMinions = 0;
    	int numMinionsDone = 0;
    	JSONArray minionsArray = new JSONArray();
    	httpResponse = Utils.getJSON(myservername + "/jobs/" + jid, null, token);
    	try {
    		// info array will tell us how many minions were targeted
    		returnArray = httpResponse.getJSONArray("info");
    		for (Object o : returnArray) {
    			JSONObject line = (JSONObject) o;
    			minionsArray = line.getJSONArray("Minions");
    			// Check the info[Minions[]] array to see how many nodes we
    			// expect to hear back from
    			numMinions = minionsArray.size();
    			listener.getLogger().println("Waiting for " + numMinions + " minions");
    		}
    		returnArray = httpResponse.getJSONArray("return");
    		// Check the return[] array to see how many minions have
    		// responded
    		if (!returnArray.getJSONObject(0).names().isEmpty()) {
    			numMinionsDone = returnArray.getJSONObject(0).names().size();
    		} else {
    			numMinionsDone = 0;
    		}
    		listener.getLogger().println(numMinionsDone + " minions are done");
    	} catch (Exception e) {
    		listener.error(httpResponse.toString(2));
    		build.setResult(Result.FAILURE);
    	}

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
    			Thread.sleep(pollTime * 1000);
    		} catch (InterruptedException ex) {
    			Thread.currentThread().interrupt();
    			// Allow user to cancel job in jenkins interface
    			throw new InterruptedException();
    		}
    		Integer oldMinionsDone = numMinionsDone;
    		httpResponse = Utils.getJSON(myservername + "/jobs/" + jid, null, token);
    		returnArray = httpResponse.getJSONArray("return");
    		numMinionsDone = returnArray.getJSONObject(0).names().size();
    		if (numMinionsDone >= oldMinionsDone && numMinionsDone < numMinions) {
    			// Some minions returned, but not all
    			// Give them minionTimeout to all return or fail build
    			try {
    				listener.getLogger().println(
    						"Some minions returned. Waiting " + minionTimeout + " seconds");
    				Thread.sleep(minionTimeout * 1000);
    			} catch (InterruptedException ex) {
    				Thread.currentThread().interrupt();
    				// Allow user to cancel job in jenkins interface
    				throw new InterruptedException();
    			}
    			httpResponse = Utils.getJSON(myservername + "/jobs/" + jid, null, token);
    			returnArray = httpResponse.getJSONArray("return");
    			numMinionsDone = returnArray.getJSONObject(0).names().size();
    			if (numMinionsDone < numMinions) {
    				JSONArray respondedMinions = returnArray.getJSONObject(0).names();
    				for (int i = 0; i < respondedMinions.size(); ++i) {
    					minionsArray.discard(respondedMinions.get(i));
    				}
    				listener.error(
    						"Minions timed out:\n" + minionsArray.toString() + "\n\n");
    	    		build.setResult(Result.FAILURE);
    	    		return returnArray;
    			}
    		}
    	}
    	return returnArray;
    }
}