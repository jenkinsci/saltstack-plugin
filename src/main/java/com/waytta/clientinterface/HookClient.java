package com.waytta.clientinterface;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

import org.kohsuke.stapler.QueryParameter;

import com.waytta.Utils;

import hudson.util.FormValidation;
import net.sf.json.JSONSerializer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSON;
import net.sf.json.JSONException;
import org.jenkinsci.Symbol;


public class HookClient extends BasicClient {
    private String post;
    private String tag;
    
    @DataBoundConstructor
    public HookClient(String post, String tag){
        this.post = post;
    	this.tag = tag;
    }
    public String getPost() {
    	return post;
    }
    
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
