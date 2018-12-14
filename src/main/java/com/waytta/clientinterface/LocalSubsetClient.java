package com.waytta.clientinterface;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.waytta.Utils;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.util.FormValidation;

public class LocalSubsetClient extends BasicClient {
    private static final long serialVersionUID = 1L;
    private String function;
    private String arguments;
    private String subset = "1";
    private String target;
    private String targettype;
    private transient String targetType;

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

    protected Object readResolve() throws IOException {
        if (targetType != null) {
            targettype = targetType;
        }
        return this;
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
