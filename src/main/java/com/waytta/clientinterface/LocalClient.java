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
	private String targetType;
	private String function;
	private String arguments;

    @DataBoundConstructor
    public LocalClient(String function, String arguments, String target, String targetType) {
    	this.function = function;
    	this.arguments = arguments;
    	this.target = target;
    	this.targetType = targetType;
    }
    public String getFunction() {
    	return function;
    }

    public String getArguments() {
    	return arguments;
    }

    public int getJobPollTime() {
        return jobPollTime;
    }

    @DataBoundSetter
    public void setJobPollTime(int jobPollTime) {
        this.jobPollTime = jobPollTime;
    }

    public int getMinionTimeout() {
        return minionTimeout;
    }

    @DataBoundSetter
    public void setMinionTimeout(int minionTimeout) {
        this.minionTimeout = minionTimeout;
    }

    public boolean getBlockbuild() {
        return blockbuild; 
    }

    @DataBoundSetter
    public void setBlockbuild(boolean blockbuild) {
        this.blockbuild = blockbuild;
    }

    public String getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
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
        	SaltAPIBuilder.DescriptorImpl sabd = (SaltAPIBuilder.DescriptorImpl) Jenkins.getInstance().getDescriptor( SaltAPIBuilder.class );
        	return sabd.getPollTime();
        }
        public int getMinionTimeout() {
            SaltAPIBuilder.DescriptorImpl sabd = (SaltAPIBuilder.DescriptorImpl) Jenkins.getInstance().getDescriptor( SaltAPIBuilder.class );
            return sabd.getMinionTimeout();
        }
    }

    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl();
}
