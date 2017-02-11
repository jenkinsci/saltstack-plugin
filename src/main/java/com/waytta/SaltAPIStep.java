package com.waytta;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.waytta.clientinterface.BasicClient;

public class SaltAPIStep extends AbstractStepImpl {
    private static final Logger LOGGER = Logger.getLogger("com.waytta.saltstack");

    private String servername;
    private String authtype;
    private BasicClient clientInterface;
    private boolean saveEnvVar = false;
    private final String credentialsId;
    
    @DataBoundConstructor
    public SaltAPIStep(String servername, String authtype, BasicClient clientInterface, String credentialsId) {
        this.servername = servername;
        this.authtype = authtype;
        this.clientInterface = clientInterface;
        this.credentialsId = credentialsId;
    }
  
    public String getServername() {
        return servername;
    }

    public String getAuthtype() {
        return authtype;
    }

    public String getTarget() {
    	return clientInterface.getTarget();
    }

    public String getTargettype() {
    	return clientInterface.getTargetType();
    }

    public String getFunction() {
        return clientInterface.getFunction();
    }

    public String getArguments() {
        return clientInterface.getArguments();
    }

    public boolean getBlockbuild() {
    	return clientInterface.getBlockbuild();
    }

    public String getBatchSize() {
    	return clientInterface.getBatchSize();
    }

    public int getJobPollTime() {
    	return clientInterface.getJobPollTime();
    }
    
    public String getMods() {
    	return clientInterface.getMods();
    }
    
    public String getPillarvalue() {
    	return clientInterface.getPillarvalue();
    }
    
    public String getSubset() {
    	return clientInterface.getSubset();
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setSaveEnvVar(boolean saveEnvVar) {
        this.saveEnvVar = saveEnvVar;
    }

    public boolean getSaveEnvVar() {
        return saveEnvVar;
    }
    
    public BasicClient getClientInterface() {
    	return clientInterface;
    }
    
    public String getPost() {
    	return clientInterface.getPost();
    }
    
    public String getTag() {
    	return clientInterface.getTag();
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
        	super(SaltAPIStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "salt";
        }
        
        @Override
        public String getDisplayName() {
            return "Send a message to Salt API";
        }
        
        public FormValidation doCheckServername(@QueryParameter String value) {
        	return SaltAPIBuilder.DescriptorImpl.doCheckServername(value);
        }
        
        public ListBoxModel doFillCredentialsIdItems(
        		@AncestorInPath Job context,
        		@QueryParameter final String credentialsId,
                @QueryParameter final String servername) {
            return SaltAPIBuilder.DescriptorImpl.doFillCredentialsIdItems(context, credentialsId, servername);
        }
        
        public FormValidation doCheckCredentialsId(@AncestorInPath Item project, @QueryParameter String value) {
        	return SaltAPIBuilder.DescriptorImpl.doCheckCredentialsId(project, value);
        }
        
        public FormValidation doTestConnection(
                @QueryParameter String servername,
                @QueryParameter String credentialsId,
                @QueryParameter String authtype,
                @AncestorInPath Item project) {
        	return SaltAPIBuilder.DescriptorImpl.doTestConnection(servername, credentialsId, authtype, project);
        }
    }
    
    public static class SaltAPIStepExecution extends AbstractSynchronousStepExecution<String> {
        @Inject
        private transient SaltAPIStep saltStep;
        
    	@StepContextParameter
        private transient Run<?, ?> run;

        @StepContextParameter
        private transient TaskListener listener;
        
    	@Override
        protected String run() throws Exception {
    		SaltAPIBuilder saltBuilder = new SaltAPIBuilder(saltStep.servername, saltStep.authtype, saltStep.clientInterface, saltStep.credentialsId);

            StandardUsernamePasswordCredentials credential = CredentialsProvider.findCredentialById(
            		saltBuilder.getCredentialsId(), StandardUsernamePasswordCredentials.class, run);
            if (credential == null) {
                listener.error("Invalid credentials");
                run.setResult(Result.FAILURE);
                return null;
            }

            // Setup connection for auth
    	    JSONObject auth = Utils.createAuthArray(credential, saltBuilder.getAuthtype());

    	    // Get an auth token
    	    String token = new String();
    	    token = Utils.getToken(saltBuilder.getServername(), auth);
    	    if (token.contains("Error")) {
    	        listener.error(token);
                run.setResult(Result.FAILURE);
                return null;
    	    }
    	    
    	    // If we got this far, auth must have been good and we've got a token
    	    JSONObject saltFunc = saltBuilder.prepareSaltFunction(run, listener, saltBuilder.getClientInterface().getDescriptor().getDisplayName(), saltBuilder.getTarget(), saltBuilder.getFunction(), saltBuilder.getArguments());
    	    LOGGER.log(Level.FINE, "Sending JSON: " + saltFunc.toString());

            JSONArray returnArray = saltBuilder.performRequest(run, token, saltBuilder.getServername(), saltFunc, listener, saltBuilder.getBlockbuild());
            LOGGER.log(Level.FINE, "Received response: " + returnArray);
            
            // Check for error and print out results
            boolean validFunctionExecution = Utils.validateFunctionCall(returnArray);
            if (!validFunctionExecution) {
                listener.error("One or more minion did not return code 0\n");
                run.setResult(Result.FAILURE);
            }
            
    		return returnArray.toString();
        }
    	
        private static final long serialVersionUID = 1L;
    }

}