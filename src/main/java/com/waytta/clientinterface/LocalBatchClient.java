package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

import org.jenkinsci.Symbol;

import com.waytta.Utils;


public class LocalBatchClient extends BasicClient {
    private static final long serialVersionUID = 1L;
    private String function;
    private String arguments;
    private String batchSize = "100%";
    private String batchWait = "";
    private String target;
    private String targettype;
    private transient String targetType;


    @DataBoundConstructor
    public LocalBatchClient(String function, String arguments, String batchSize, String batchWait, String target, String targettype) {
        this.function = function;
        this.arguments = arguments;
        this.batchSize = batchSize;
        this.batchWait = batchWait;
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
    public String getBatchSize() {
        return batchSize;
    }
    
    @Override
    public String getBatchWait() {
        return batchWait;
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

    @Symbol("batch") @Extension
    public static final class DescriptorImpl extends BasicClientDescriptor {
        public DescriptorImpl() {
            super(LocalBatchClient.class);
        }

        @Override
        public String getDisplayName() {
            return "local_batch";
        }

        public FormValidation doCheckFunction(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt function", "Isn't it too short?");
        }

        public FormValidation doCheckBatchSize(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please specify batch size");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTarget(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a salt target", "Isn't it too short?");
        }
    }

    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl();
}
