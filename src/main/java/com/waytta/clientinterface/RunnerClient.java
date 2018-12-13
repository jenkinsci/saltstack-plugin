package com.waytta.clientinterface;

import com.waytta.Utils;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.util.FormValidation;

public class RunnerClient extends BasicClient {
    private static final long serialVersionUID = 1L;
    private String function;
    private String arguments;
    private String mods = "";
	private String pillarvalue = "";

    @DataBoundConstructor
    public RunnerClient(String function, String arguments, String mods, String pillarvalue){
    	this.function = function;
    	this.arguments = arguments;
    	this.pillarvalue = pillarvalue;
    	this.mods = mods;
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
    public String getPillarvalue() {
        return pillarvalue;
    }

    @Override
    public String getMods() {
        return mods;
    }

    @Symbol("runner") @Extension
    public static final class DescriptorImpl extends BasicClientDescriptor {
        public DescriptorImpl() {
            super(RunnerClient.class);
        }

        @Override
        public String getDisplayName() {
        	return "runner";
        }

        public FormValidation doCheckFunction(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt function", "Isn't it too short?");
        }

        public FormValidation doCheckPillarvalue(@QueryParameter String value) {
        	return Utils.validatePillar(value);
        }
    }

    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl();
}
