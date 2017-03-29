package com.waytta.clientinterface;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.DescriptorExtensionList;
import java.util.List;
import hudson.util.ListBoxModel;


abstract public class BasicClient implements ExtensionPoint, Describable<BasicClient> {
	public String getFunction() {
		return "";
	}
	
	public String getArguments() {
		return "";
	}
	
	public String getTarget() {
		return "";
	}
	
	public String getTargetType() {
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
	
	public Descriptor<BasicClient> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }
    
    public List<BasicClientDescriptor> getClientDescriptors() {
    	return Jenkins.getInstance().getDescriptorList(BasicClient.class);
    }

    abstract public static class BasicClientDescriptor extends Descriptor<BasicClient> {
        public BasicClientDescriptor(Class<? extends BasicClient> clazz) {
            super(clazz);
        }

        abstract public String getDisplayName();
        
        public ListBoxModel doFillTargetTypeItems() {
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
