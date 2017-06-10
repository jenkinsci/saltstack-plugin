package com.waytta;

import com.waytta.SaltException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import javax.inject.Inject;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Job;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.waytta.clientinterface.BasicClient;

import com.google.common.collect.ImmutableSet;

public class SaltAPIStep extends Step implements Serializable {
    private static final Logger LOGGER = Logger.getLogger("com.waytta.saltstack");

    private String servername;
    private String authtype;
    private BasicClient clientInterface;
    private boolean saveEnvVar = false;
    private final String credentialsId;
    private boolean saveFile = false;
    private static String token = null;
    private static String netapi = null;
    private static JSONObject saltFunc = null;

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
        return clientInterface.getTargettype();
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

    public int getMinionTimeout() {
        return clientInterface.getMinionTimeout();
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

    @DataBoundSetter
    public void setSaveFile(boolean saveFile) {
        this.saveFile = saveFile;
    }

    public boolean getSaveFile() {
        return saveFile;
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
    public static final class DescriptorImpl extends StepDescriptor {

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

        @Override
        public Set<Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, FilePath.class, TaskListener.class, Launcher.class);
        }
    }

    @Override public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }

    public class Execution extends AbstractStepExecutionImpl {
        private static final long serialVersionUID = 1L;

        private String jid;

        @Inject
        private SaltAPIStep saltStep;

        private transient volatile ScheduledFuture<?> task;

        SaltAPIBuilder saltBuilder;

        Execution(SaltAPIStep step, StepContext context) {
            super(context);
            this.saltStep = step;
        }

        @Override
        public void stop(Throwable cause) throws Exception {
            if (task != null) {
                task.cancel(false);
            }
            getContext().onFailure(cause);
        }

        @Override
        public boolean start() throws Exception {
            Launcher launcher = getContext().get(Launcher.class);
            TaskListener listener = getContext().get(TaskListener.class);

            prepareRun();
            jid = saltBuilder.getJID(launcher, saltBuilder.getServername(), token, saltFunc, listener);

            new Thread("saltAPI") {
                @Override
                public void run() {
                    try {
                        saltPerform(token, saltFunc, netapi);
                    }
                    catch (Exception e) {
                        Execution.this.getContext().onFailure(e);
                    }
                }
            }.start();

            return false;
        }

        @Override public void onResume() {
            TaskListener listener = null;
            Launcher launcher = null;
            FilePath workspace = null;
            try {
                listener = getContext().get(TaskListener.class);
                launcher = getContext().get(Launcher.class);
                workspace = getContext().get(FilePath.class);
            } catch (Exception e) {
                Execution.this.getContext().onFailure(e);
            }

            // Fail out if missing jid
            if (jid == null || jid.equals("")) {
                throw new RuntimeException("Unable to resume. Missing JID.");
            }

            listener.getLogger().println("Resuming jid: " + jid);

            // Auth to salt-api
            try {
                prepareRun();
            } catch (Exception e) {
                Execution.this.getContext().onFailure(e);
            }

            // Poll for completion
            int jobPollTime = saltBuilder.getJobPollTime();
            int minionTimeout = saltBuilder.getMinionTimeout();
            JSONArray returnArray = null;
            try {
                returnArray = Builds.checkBlockingBuild(launcher, saltBuilder.getServername(), token, saltFunc, listener, jobPollTime, minionTimeout, netapi, jid);
            } catch (Exception e) {
                Execution.this.getContext().onFailure(e);
            }

            // Verify and return result
            postRun(returnArray);
        }

        private void prepareRun() throws InterruptedException, IOException{
            Run<?, ?>run = getContext().get(Run.class);
            TaskListener listener = getContext().get(TaskListener.class);
            Launcher launcher = getContext().get(Launcher.class);

            saltBuilder = new SaltAPIBuilder(saltStep.servername, saltStep.authtype, saltStep.clientInterface, saltStep.credentialsId);

            StandardUsernamePasswordCredentials credential = CredentialsProvider.findCredentialById(
                    saltBuilder.getCredentialsId(), StandardUsernamePasswordCredentials.class, run);
            if (credential == null) {
                throw new RuntimeException("Invalid credentials");
            }

            // Setup connection for auth
            JSONObject auth = Utils.createAuthArray(credential, saltBuilder.getAuthtype());

            // Get an auth token
            ServerToken serverToken = Utils.getToken(launcher, saltBuilder.getServername(), auth);
            token = serverToken.getToken();
            netapi = serverToken.getServer();
            LOGGER.log(Level.FINE, "Discovered netapi: " + netapi);

            // If we got this far, auth must have been good and we've got a token
            saltFunc = saltBuilder.prepareSaltFunction(run, listener, saltBuilder.getClientInterface().getDescriptor().getDisplayName(), saltBuilder.getTarget(), saltBuilder.getFunction(), saltBuilder.getArguments());
            LOGGER.log(Level.FINE, "Sending JSON: " + saltFunc.toString());
        }

        private void saltPerform(String token, JSONObject saltFunc, String netapi) throws Exception, SaltException {
            Run<?, ?>run = getContext().get(Run.class);
            FilePath workspace = getContext().get(FilePath.class);
            TaskListener listener = getContext().get(TaskListener.class);
            Launcher launcher = getContext().get(Launcher.class);

            JSONArray returnArray = null;
            if (jid != null && !jid.equals("")) {
                returnArray = Builds.checkBlockingBuild(launcher, saltBuilder.getServername(), token, saltFunc, listener, saltBuilder.getJobPollTime(), saltBuilder.getMinionTimeout(), netapi, jid);
            } else {
                returnArray = saltBuilder.performRequest(launcher, run, token, saltBuilder.getServername(), saltFunc, listener, netapi);
            }
            postRun(returnArray);
        }

        private void postRun(JSONArray returnArray) {
            TaskListener listener = null;
            FilePath workspace = null;
            try {
                listener = getContext().get(TaskListener.class);
                workspace = getContext().get(FilePath.class);
            } catch (Exception e) {
                Execution.this.getContext().onFailure(e);
            }

            LOGGER.log(Level.FINE, "Received response: " + returnArray);

            // Check for error and print out results
            boolean validFunctionExecution = Utils.validateFunctionCall(returnArray);
            if (!validFunctionExecution) {
                listener.error("One or more minion did not return code 0\n");
                Execution.this.getContext().onFailure(new SaltException(returnArray.toString()));
            }

            if (saltStep.saveFile) {
                try {
                    Utils.writeFile(returnArray.toString(), workspace);
                } catch (Exception e) {
                    Execution.this.getContext().onFailure(e);
                }
            }

            // Return results
            getContext().onSuccess(returnArray.toString());
        }
    }

}