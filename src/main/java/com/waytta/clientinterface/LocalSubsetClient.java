package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.QueryParameter;
import hudson.util.FormValidation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.DataBoundConstructor;

import com.waytta.Utils;


public class LocalSubsetClient extends BasicClient {
	private String function;
	private String arguments;
	private String subset = "1";
	private String target;
	private String targettype;

    @DataBoundConstructor
    public LocalSubsetClient(String function, String arguments, String subset, String target, String targettype) {
    	this.function = function;
    	this.arguments = arguments;
    	this.subset = subset;
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
    public String getSubset() {
    	return subset;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public String getTargettype() {
        return targettype;
    }

    @Symbol("subset") @Extension
    public static class DescriptorImpl extends BasicClientDescriptor {
    	public DescriptorImpl() {
            super(LocalSubsetClient.class);
        }

        @Override
        public String getDisplayName() {
        	return "local_subset";
        }

        public FormValidation doCheckFunction(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt function", "Isn't it too short?");
        }

        public FormValidation doCheckTarget(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt target", "Isn't it too short?");
        }

        public FormValidation doCheckSubset(@QueryParameter String value) {
        	// Check to see if paramorized. Ex: {{variable}}
        	// This cannot be evaluated until build, so trust that all is well
        	Pattern pattern = Pattern.compile("\\{\\{\\w+\\}\\}");
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches()) {
            	return FormValidation.ok();
            }
        	try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return FormValidation.error("Specify a number");
            }

            return FormValidation.ok();
        }
    }

    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl();
}
