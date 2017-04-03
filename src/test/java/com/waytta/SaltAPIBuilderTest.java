package com.waytta;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Launcher;
import hudson.remoting.Callable;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
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
@PrepareForTest({JSONObject.class, Jenkins.class, SaltAPIBuilder.DescriptorImpl.class, CredentialsProvider.class, Utils.class})
public class SaltAPIBuilderTest {

    private static final Integer TESTINT = (int)(new Date().getTime());
    private static final Integer TEN = new Integer(10);
    private static final String DEFAULT_CREDENTIAL_ID = "credentials_id";
    private static final hudson.util.Secret DEFAULT_CREDENTIAL_PASSWORD = Secret.fromString("junit_password");
    private static final String JSON_FORMAT = "json";
    BasicClient clientInterfaces;
    private List<StandardUsernamePasswordCredentials> credentials;


    @Before
    public void setup(){
        //clientInterfaces = mock(JSONObject.class);
        //when(clientInterfaces.has("clientInterface")).thenReturn(TRUE);
        credentials = new ArrayList<StandardUsernamePasswordCredentials>();
    }


    private void validateBuilder(SaltAPIBuilder builder,
                                 String clientInterfaces,
                                 String mods,
                                 String pillarValue){
        validateBuilder(builder, clientInterfaces, TEN, "100%", mods, pillarValue );
    }
    private void validateBuilder(SaltAPIBuilder builder,
                                 String clientInterface) {
        validateBuilder( builder, clientInterface, TEN, "100%", "", "");
    }

    private void validateBuilder(String clientInterface,
                                 SaltAPIBuilder builder,
                                 String mods) {
        validateBuilder( builder, clientInterface, TEN, "100%", mods, "");
    }

    private void validateBuilder(SaltAPIBuilder builder,
                                 String clientInterface,
                                 String batchSize) {
        validateBuilder( builder, clientInterface, TEN, batchSize, "", "");
    }

    private void validateBuilder(SaltAPIBuilder builder,
                                 String clientInterface,
                                 Integer jobPollTime) {
        validateBuilder( builder, clientInterface, jobPollTime, "100%", "", "");
    }

    private void validateBuilder(SaltAPIBuilder builder,
                                 String clientInterface,
                                 Integer jobPollTime,
                                 String batchSize,
                                 String mods,
                                 String pillarValue) {
        assertFalse(builder.getBlockbuild());
        assertEquals(clientInterface, builder.getClientInterface());
        assertEquals(batchSize, builder.getBatchSize());
        //assertEquals(jobPollTime, builder.getJobPollTime());
        assertEquals(mods, builder.getMods());
        assertEquals(pillarValue, builder.getPillarvalue());
    }

    SaltAPIBuilder build() {
        return new SaltAPIBuilder(
                "servername",
                "authtype",
                clientInterfaces,
                DEFAULT_CREDENTIAL_ID);
    }

    @Test
    public void testPerformWithNoCredentials() throws Exception {
        SaltAPIBuilder builder = setupBuilderForDefaultPerform();
        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        Launcher launcher = mock(Launcher.class);
        BuildListener buildListener = mock(BuildListener.class);

        // TODO fix test workspace errors
        //assertFalse(builder.perform(jenkinsBuild, launcher, buildListener));
    }

    @Test
    public void testPerformWithFailingCredentials() throws Exception {
        StandardUsernamePasswordCredentials mockCred = mock(StandardUsernamePasswordCredentials.class);
        when(mockCred.getId()).thenReturn(DEFAULT_CREDENTIAL_ID);
        when(mockCred.getPassword()).thenReturn(DEFAULT_CREDENTIAL_PASSWORD);
        credentials.add(mockCred);

        SaltAPIBuilder builder = setupBuilderForDefaultPerform();

        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        Launcher launcher = mock(Launcher.class);

        // TODO fix test workspace errors
        //assertFalse(builder.perform(jenkinsBuild, launcher, buildListener));
    }

    @Test
    public void testPerformWithPassingCredentials() throws Exception {
        StandardUsernamePasswordCredentials mockCred = mock(StandardUsernamePasswordCredentials.class);
        when(mockCred.getId()).thenReturn(DEFAULT_CREDENTIAL_ID);
        when(mockCred.getPassword()).thenReturn(DEFAULT_CREDENTIAL_PASSWORD);
        credentials.add(mockCred);

        SaltAPIBuilder builder = setupBuilderForDefaultPerform();
        mockStatic(Utils.class);
        when(Utils.getToken(any(Launcher.class), anyString(), any(JSONObject.class))).thenReturn(new ServerToken(null, null));

        BuildListener buildListener = mock(BuildListener.class);
        PrintStream printer = mock(PrintStream.class);
        when(buildListener.getLogger()).thenReturn(printer);

        AbstractBuild jenkinsBuild = mock(AbstractBuild.class);
        Launcher launcher = mock(Launcher.class);
        //when(Utils.paramorize(jenkinsBuild, buildListener, builder.getTarget())).thenReturn("junit_mytarget");
        //when(Utils.paramorize(jenkinsBuild, buildListener, builder.getFunction())).thenReturn("junit_myfunction");
        //when(Utils.paramorize(jenkinsBuild, buildListener, builder.getArguments())).thenReturn("junit_myarguments");
        JSONObject httpResponse = mock(JSONObject.class);
        //when(launcher.getChannel().call(new saltAPI(anyString(), any(JSONObject.class),anyString()))).thenReturn(httpResponse);
        when(httpResponse.toString(2)).thenReturn("junit_httpResponse");
        JSONArray returnArray = new JSONArray();
        returnArray.add(0,"junit");
        when(httpResponse.getJSONArray("return")).thenReturn(returnArray);
        when(Utils.validateFunctionCall(returnArray)).thenReturn(true);

        // TODO fix test workspace errors
        //assertTrue(builder.perform(jenkinsBuild, launcher, buildListener));
    }

    private SaltAPIBuilder setupBuilderForDefaultPerform() {
        return setupBuilderForDefaultPerform(JSON_FORMAT);
    }

    private SaltAPIBuilder setupBuilderForDefaultPerform(String outputFormat) {

        //when(clientInterfaces.get("clientInterface")).thenReturn("JUNIT");
        SaltAPIBuilder.DescriptorImpl descriptor = mock(SaltAPIBuilder.DescriptorImpl.class);
        mockStatic(Jenkins.class);
        Jenkins jenkins = mock(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        when(jenkins.getDescriptorOrDie((Class<? extends hudson.model.Describable>) any())).thenReturn(descriptor);
        when(descriptor.getOutputFormat()).thenReturn(outputFormat);

        mockStatic(CredentialsProvider.class);
        when(CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class, jenkins, ACL.SYSTEM, new ArrayList<DomainRequirement>())).thenReturn(credentials);;

        SaltAPIBuilder builder = build();

        return builder;
    }
}
