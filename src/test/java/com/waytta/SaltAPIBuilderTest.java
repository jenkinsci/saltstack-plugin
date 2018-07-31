package com.waytta;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Launcher;
import hudson.remoting.Callable;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.waytta.clientinterface.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JSONObject.class, Jenkins.class, SaltAPIBuilder.DescriptorImpl.class, CredentialsProvider.class,
        Utils.class })
public class SaltAPIBuilderTest {

    private String target = "*";
    private String function = "cmd.run";
    private String arguments = "'ls -la'";
    private String targettype = "glob";
    private String mods = "";
    private String pillarvalue = "{\"key\":\"value\"}";

    @Test
    public void testHookPrepareSaltFunction() throws Exception {
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        BuildListener buildListener = mock(BuildListener.class);

        HookClient client = new HookClient("{\"key3\":\"value\"}", "/test/url");
        String myClientInterface = "hook";
        SaltAPIBuilder saltAPIBuilder = new SaltAPIBuilder("name", "pam", client, "creds");

        JSONObject testObject = JSONObject.fromObject("{\"key3\":\"value\"}");

        JSONObject result = saltAPIBuilder.prepareSaltFunction(jenkinsBuild, buildListener, myClientInterface, target,
                function, arguments, "older", targettype);
        assertEquals(testObject, result);
    }

    @Test
    public void testLocalBatchClientPrepareSaltFunction() throws Exception {
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        LocalBatchClient client = new LocalBatchClient(function, arguments, "50%", target, targettype);
        String myClientInterface = "local_batch";
        SaltAPIBuilder saltAPIBuilder = new SaltAPIBuilder("name", "pam", client, "creds");

        JSONObject testObject = JSONObject.fromObject("{"
                + "\"client\":\"local_batch\","
                + "\"batch\":\"50%\","
                + "\"tgt\":\"*\","
                + "\"expr_form\":\"glob\","
                + "\"fun\":\"cmd.run\","
                + "\"arg\":\"ls -la\","
                + "\"kwarg\":{}"
                + "}");

        JSONObject result = saltAPIBuilder.prepareSaltFunction(jenkinsBuild, buildListener, myClientInterface, target,
                function, arguments, "older", targettype);
        assertEquals(testObject, result);
    }

    @Test
    public void testLocalBatchClientPrepareSaltFunction2017() throws Exception {
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        LocalBatchClient client = new LocalBatchClient(function, arguments, "50%", target, targettype);
        String myClientInterface = "local_batch";
        SaltAPIBuilder saltAPIBuilder = new SaltAPIBuilder("name", "pam", client, "creds");

        JSONObject testObject = JSONObject.fromObject("{"
                + "\"client\":\"local_batch\","
                + "\"batch\":\"50%\","
                + "\"tgt\":\"*\","
                + "\"tgt_type\":\"glob\","
                + "\"fun\":\"cmd.run\","
                + "\"full_return\":true,"
                + "\"arg\":\"ls -la\","
                + "\"kwarg\":{}"
                + "}");

        JSONObject result = saltAPIBuilder.prepareSaltFunction(jenkinsBuild, buildListener, myClientInterface, target,
                function, arguments, "2017.7", targettype);
        assertEquals(testObject, result);
    }

    @Test
    public void testLocalClientPrepareSaltFunction() throws Exception {
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        LocalClient client = new LocalClient(function, arguments, target, targettype);
        String myClientInterface = "local";
        SaltAPIBuilder saltAPIBuilder = new SaltAPIBuilder("name", "pam", client, "creds");

        JSONObject testObject = JSONObject.fromObject("{"
                + "\"client\":\"local\","
                + "\"tgt\":\"*\","
                + "\"expr_form\":\"glob\","
                + "\"fun\":\"cmd.run\","
                + "\"arg\":\"ls -la\","
                + "\"kwarg\":{}"
                + "}");

        JSONObject result = saltAPIBuilder.prepareSaltFunction(jenkinsBuild, buildListener, myClientInterface, target,
                function, arguments, "older", targettype);
        assertEquals(testObject, result);
    }

    @Test
    public void testLocalClientPrepareSaltFunction2017() throws Exception {
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        LocalClient client = new LocalClient(function, arguments, target, targettype);
        String myClientInterface = "local";
        SaltAPIBuilder saltAPIBuilder = new SaltAPIBuilder("name", "pam", client, "creds");

        JSONObject testObject = JSONObject.fromObject("{"
                + "\"client\":\"local\","
                + "\"tgt\":\"*\","
                + "\"tgt_type\":\"glob\","
                + "\"fun\":\"cmd.run\","
                + "\"full_return\":true,"
                + "\"arg\":\"ls -la\","
                + "\"kwarg\":{}"
                + "}");

        JSONObject result = saltAPIBuilder.prepareSaltFunction(jenkinsBuild, buildListener, myClientInterface, target,
                function, arguments, "2017.7", targettype);
        assertEquals(testObject, result);
    }

    @Test
    public void testLocalAsyncClientPrepareSaltFunction() throws Exception {
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        LocalClient client = mock(LocalClient.class);
        when(client.getBlockbuild()).thenReturn(TRUE);
        when(client.getTargettype()).thenReturn("glob");
        when(client.getFunction()).thenReturn("test.ping");
        when(client.getTarget()).thenReturn("minion1");

        String myClientInterface = "local";
        SaltAPIBuilder saltAPIBuilder = new SaltAPIBuilder("name", "pam", client, "creds");

        JSONObject testObject = JSONObject.fromObject("{"
                + "\"client\":\"local_async\","
                + "\"tgt\":\"*\","
                + "\"expr_form\":\"glob\","
                + "\"fun\":\"cmd.run\","
                + "\"arg\":\"ls -la\","
                + "\"kwarg\":{}"
                + "}");

        JSONObject result = saltAPIBuilder.prepareSaltFunction(jenkinsBuild, buildListener, myClientInterface, target,
                function, arguments, "older", targettype);
        assertEquals(testObject, result);
    }

    @Test
    public void testLocalAsyncClientPrepareSaltFunction2017() throws Exception {
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        LocalClient client = mock(LocalClient.class);
        when(client.getBlockbuild()).thenReturn(TRUE);
        when(client.getTargettype()).thenReturn("glob");
        when(client.getFunction()).thenReturn("test.ping");
        when(client.getTarget()).thenReturn("minion1");

        String myClientInterface = "local";
        SaltAPIBuilder saltAPIBuilder = new SaltAPIBuilder("name", "pam", client, "creds");

        JSONObject testObject = JSONObject.fromObject("{"
                + "\"client\":\"local_async\","
                + "\"tgt\":\"*\","
                + "\"tgt_type\":\"glob\","
                + "\"fun\":\"cmd.run\","
                + "\"full_return\":true,"
                + "\"arg\":\"ls -la\","
                + "\"kwarg\":{}"
                + "}");

        JSONObject result = saltAPIBuilder.prepareSaltFunction(jenkinsBuild, buildListener, myClientInterface, target,
                function, arguments, "2017.7", targettype);
        assertEquals(testObject, result);
    }

    @Test
    public void testLocalSubsetClientPrepareSaltFunction() throws Exception {
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        LocalSubsetClient client = new LocalSubsetClient(function, arguments, "5", target, targettype);
        String myClientInterface = "local_subset";
        SaltAPIBuilder saltAPIBuilder = new SaltAPIBuilder("name", "pam", client, "creds");

        JSONObject testObject = JSONObject.fromObject("{"
                + "\"client\":\"local_subset\","
                + "\"sub\":\"5\","
                + "\"tgt\":\"*\","
                + "\"expr_form\":\"glob\","
                + "\"fun\":\"cmd.run\","
                + "\"arg\":\"ls -la\","
                + "\"kwarg\":{}"
                + "}");

        JSONObject result = saltAPIBuilder.prepareSaltFunction(jenkinsBuild, buildListener, myClientInterface, target,
                function, arguments, "older", targettype);
        assertEquals(testObject, result);
    }

    @Test
    public void testLocalSubsetClientPrepareSaltFunction2017() throws Exception {
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        LocalSubsetClient client = new LocalSubsetClient(function, arguments, "5", target, targettype);
        String myClientInterface = "local_subset";
        SaltAPIBuilder saltAPIBuilder = new SaltAPIBuilder("name", "pam", client, "creds");

        JSONObject testObject = JSONObject.fromObject("{"
                + "\"client\":\"local_subset\","
                + "\"sub\":\"5\","
                + "\"tgt\":\"*\","
                + "\"tgt_type\":\"glob\","
                + "\"fun\":\"cmd.run\","
                + "\"full_return\":true,"
                + "\"arg\":\"ls -la\","
                + "\"kwarg\":{}" + "}");

        JSONObject result = saltAPIBuilder.prepareSaltFunction(jenkinsBuild, buildListener, myClientInterface, target,
                function, arguments, "2017.7", targettype);
        assertEquals(testObject, result);
    }

    @Test
    public void testRunnerClientPrepareSaltFunction() throws Exception {
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        RunnerClient client = new RunnerClient(function, arguments, mods, pillarvalue);
        String myClientInterface = "runner";
        SaltAPIBuilder saltAPIBuilder = new SaltAPIBuilder("name", "pam", client, "creds");

        JSONObject testObject = JSONObject.fromObject("{"
                + "\"client\":\"runner\","
                + "\"pillar\":{\"key\":\"value\"},"
                + "\"fun\":\"cmd.run\","
                + "\"arg\":\"ls -la\","
                + "\"kwarg\":{}"
                + "}");

        JSONObject result = saltAPIBuilder.prepareSaltFunction(jenkinsBuild, buildListener, myClientInterface, target,
                function, arguments, "older", targettype);
        assertEquals(testObject, result);
    }
}
