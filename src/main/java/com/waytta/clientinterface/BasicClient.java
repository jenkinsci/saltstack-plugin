package com.waytta.clientinterface;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import java.io.Serializable;
import java.util.List;
import hudson.util.ListBoxModel;


abstract public class BasicClient implements ExtensionPoint, Serializable, Describable<BasicClient> {
    private static final long serialVersionUID = 1L;

    public String getFunction() {
        return "";
    }

    public String getArguments() {
        return "";
    }

    public String getTarget() {
        return "";
    }

    public String getTargettype() {
        return "";
    }

    public boolean getBlockbuild() {
        return false;
    }

    public String getBatchSize() {
        return "";
    }

    public int getJobPollTime() {
        return 10;
    }

    public int getMinionTimeout() {
        return 30;
    }

    public String getMods() {
        return "";
    }

    public String getPillarvalue() {
        return "";
    }

    public String getSubset() {
        return "1";
    }

    public String getTag() {
        return "";
    }

    public String getPost() {
        return "";
    }

    @Override
    public Descriptor<BasicClient> getDescriptor() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not been started, or was already shut down");
        }
        return jenkins.getDescriptorOrDie(getClass());
    }

    public List<BasicClientDescriptor> getClientDescriptors() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not been started, or was already shut down");
        }
        return jenkins.getDescriptorList(BasicClient.class);
    }

    abstract public static class BasicClientDescriptor extends Descriptor<BasicClient> {
        public BasicClientDescriptor(Class<? extends BasicClient> clazz) {
            super(clazz);
        }

        @Override
        abstract public String getDisplayName();

        public ListBoxModel doFillTargettypeItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("glob", "glob");
            items.add("pcre", "pcre");
            items.add("list", "list");
            items.add("grain", "grain");
            items.add("pillar", "pillar");
            items.add("nodegroup", "nodegroup");
            items.add("range", "range");
            items.add("compound", "compound");

            return items;
        }
    }
}
