package com.waytta.clientinterface;

import com.waytta.Utils;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.util.FormValidation;

public class HookClient extends BasicClient {
    private static final long serialVersionUID = 1L;
    private String post;
    private String tag;

    @DataBoundConstructor
    public HookClient(String post, String tag){
        this.post = post;
    	this.tag = tag;
    }
    @Override
    public String getPost() {
    	return post;
    }

    @Override
    public String getTag() {
    	return tag;
    }

    @Symbol("hook") @Extension
    public static final class DescriptorImpl extends BasicClientDescriptor {
        public DescriptorImpl() {
            super(HookClient.class);
        }

        @Override
        public String getDisplayName() {
        	return "hook";
        }

        public FormValidation doCheckTag(@QueryParameter String value) {
            return Utils.validateFormStringField(value, "Please specify a tag", "Isn't it too short?");
        }

        public FormValidation doCheckPost(@QueryParameter String value) {
        	return Utils.validatePillar(value);
        }
    }

    public static final BasicClientDescriptor DESCRIPTOR = new DescriptorImpl();
}
