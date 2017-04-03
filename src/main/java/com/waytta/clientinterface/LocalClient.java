package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.QueryParameter;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.DataBoundConstructor;

import com.waytta.Utils;
import com.waytta.SaltAPIBuilder;
import jenkins.model.Jenkins;


public class LocalClient extends BasicClient {
	public static final int DEFAULT_JOB_POLL_TIME = 10;
	private int jobPollTime = DEFAULT_JOB_POLL_TIME;
	private int minionTimeout = 30;
	private boolean blockbuild = false;
	private String target;
	private String targettype;
	private String function;
	private String arguments;

    @DataBoundConstructor
    public LocalClient(String function, String arguments, String target, String targettype) {
    	this.function = function;
    	this.arguments = arguments;
    	this.target = target;
    	this.targettype = targettype;
    }
    @Override
    public String getFunction() {
    	return function;
    }

    @Override
    public String getArguments() {
    	return arguments;
    }

    @Override
    public int getJobPollTime() {
        return jobPollTime;
    }

    @DataBoundSetter
    public void setJobPollTime(int jobPollTime) {
        this.jobPollTime = jobPollTime;
    }

    @Override
    public int getMinionTimeout() {
        return minionTimeout;
    }

    @DataBoundSetter
    public void setMinionTimeout(int minionTimeout) {
        this.minionTimeout = minionTimeout;
    }

    @Override
    public boolean getBlockbuild() {
        return blockbuild;
    }

    @DataBoundSetter
    public void setBlockbuild(boolean blockbuild) {
        this.blockbuild = blockbuild;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public String getTargettype() {
        return targettype;
    }

    @Symbol("local") @Extension
    public static class DescriptorImpl extends BasicClientDescriptor {
    	public DescriptorImpl() {
            super(LocalClient.class);
        }

        @Override
        public String getDisplayName() {
        	return "local";
        }

        public FormValidation doCheckFunction(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt function", "Isn't it too short?");
        }

        public FormValidation doCheckTarget(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt target", "Isn't it too short?");
        }

        // Set default to global default
        public int getJobPollTime() {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                throw new IllegalStateException("Jenkins has not been started, or was already shut down");
            }
            SaltAPIBuilder.DescriptorImpl sabd = (SaltAPIBuilder.DescriptorImpl) jenkins.getDescriptor( SaltAPIBuilder.class );
            return sabd.getPollTime();
        }
        public int getMinionTimeout() {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                throw new IllegalStateException("Jenkins has not been started, or was already shut down");
            }
            SaltAPIBuilder.DescriptorImpl sabd = (SaltAPIBuilder.DescriptorImpl) jenkins.getDescriptor( SaltAPIBuilder.class );
            return sabd.getMinionTimeout();
        }
    }

    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl();
}
