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
    private String subset = "1";
    private String target;
    private String targetType;

    @DataBoundConstructor
    public LocalSubsetClient(String subset, String target, String targetType) {
        this.subset = subset;
    	this.target = target;
        this.targetType = targetType;
    }

    public String getSubset() {
    	return subset;
    }
    
    public String getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
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
