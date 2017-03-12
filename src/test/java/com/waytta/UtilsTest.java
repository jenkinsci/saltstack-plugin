package com.waytta;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

import net.sf.json.JSONArray;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.EnvVars;
import hudson.model.Run;

import static org.mockito.Mockito.mock;
import org.mockito.Mock;
import static org.mockito.Mockito.when;

import org.jvnet.hudson.test.HudsonTestCase;



public class UtilsTest extends HudsonTestCase {
	@Test
	public static void testValidPillar() {
		String value = "{\"key\": \"value\"}";
		FormValidation formValidation = Utils.validatePillar(value);
		assertEquals(FormValidation.Kind.OK, formValidation.kind);
	}

	@Test
	public static void testInvalidPillar() {
		String value = "{\"key\": value}";
		FormValidation formValidation = Utils.validatePillar(value);
		assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
	}

    @Test
    public void testValidValidateFunctionCall() {
        JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\"sql.stg.local\": {\n" +
                "  \"postgres_database_|-smth-db_|-smth_|-absent\":   {\n" +
                "    \"comment\": \"Database smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.253964\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 207.749,\n" +
                "    \"__run_num__\": 0,\n" +
                "    \"changes\": {\"smth\": \"Absent\"}\n" +
                "  },\n" +
                "  \"postgres_user_|-smth-db-user_|-smth_|-absent\":   {\n" +
                "    \"comment\": \"User smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.462937\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 837.302,\n" +
                "    \"__run_num__\": 1,\n" +
                "    \"changes\": {\"smth\": \"Absent\"}\n" +
                "  }\n" +
                "}}}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testValidValidateFunctionCallForSimpleObject() {
        JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\n" +
                "    \"comment\": \"User smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.462937\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 837.302,\n" +
                "    \"__run_num__\": 1,\n" +
                "    \"changes\": {\"smth\": \"Absent\"}\n" +
                "}}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testValidValidateFunctionCallForSimpleObjectWithOkReturnCode() {
        JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\n" +
                "    \"comment\": \"User smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.462937\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 837.302,\n" +
                "    \"__run_num__\": 1,\n" +
                "    \"changes\": {\"smth\": \"Absent\"},\n" +
                "    \"retcode\":0\n" +
                "}}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testInvalidValidateFunctionCall() {
        JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\"sql.stg.local\": {\n" +
                "  \"postgres_database_|-smth-db_|-smth_|-absent\":   {\n" +
                "    \"comment\": \"Database smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.253964\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 207.749,\n" +
                "    \"__run_num__\": 0,\n" +
                "    \"changes\": {\"smth\": \"Absent\"}\n" +
                "  },\n" +
                "  \"postgres_user_|-smth-db-user_|-smth_|-absent\":   {\n" +
                "    \"comment\": \"User smth has been removed\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.462937\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 837.302,\n" +
                "    \"__run_num__\": 1,\n" +
                "    \"changes\": {\"smth\": \"Absent\"},\n" +
                "    \"retcode\":2\n" +
                "  }\n" +
                "}}}]");

        Assert.assertFalse(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testInvalidValidateFunctionCallForSimpleObject() {
        JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\n" +
                "    \"comment\": \"User made action\",\n" +
                "    \"name\": \"smth\",\n" +
                "    \"start_time\": \"08:24:29.462937\",\n" +
                "    \"result\": true,\n" +
                "    \"duration\": 837.302,\n" +
                "    \"__run_num__\": 1,\n" +
                "    \"changes\": {\"smth\": \"Absent\"},\n" +
                "    \"retcode\":2\n" +
                "}}]");

        Assert.assertFalse(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testValidateFunctionCallForFailedHighstate() {
	JSONArray jsonArray = JSONArray.fromObject("[{\"data\": {\n" +
		"\"minionname\": {\n" +
		"  \"cmd_|-fails_|-/bin/false_|-run\": {\n" +
		"    \"__run_num__\": 0,\n" +
		"    \"_stamp\": \"2016-02-23T21:15:55.813678\",\n" +
		"    \"changes\": {\n" +
		"      \"pid\": 16745,\n" +
		"      \"retcode\": 1,\n" +
		"      \"stderr\": \"\",\n" +
		"      \"stdout\": \"\" },\n" +
		"    \"comment\": \"Command \\\"/bin/false\\\" run\",\n" +
		"    \"duration\": 17.302,\n" +
		"    \"fun\": \"state.sls\",\n" +
		"    \"id\": \"minionname\",\n" +
		"    \"jid\": \"20160223151555485695\",\n" +
		"    \"name\": \"/bin/false\",\n" +
		"    \"start_time\": \"15:15:29.462937\",\n" +
		"    \"result\": false,\n" +
		"    \"retcode\":2,\n" +
		"    \"return\": \"Error: cmd.run\",\n" +
		"    \"success\": false\n" +
		"}}}}]");

	Assert.assertFalse(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testInvalidValidateFunctionCallForMissingPillar() {
        JSONArray jsonArray = JSONArray.fromObject("[{\n" +
	        "\"data\": {\n" +
		  "\"minionname\": [\n" +
                  "    \"Rendering SLS 'base:failures.pillar' failed: Jinja variable 'dict object' has no attribute 'nope'\"\n" +
                  "]},\n" +
		"\"outputter\":\"highstate\"\n" +
		"}]");

        Assert.assertFalse(Utils.validateFunctionCall(jsonArray));
    }


    @Test
    public void testInvalidValidateFunctionCallForDuplicateStateName() {
        JSONArray jsonArray = JSONArray.fromObject("[{\n" +
		"\"minionname\": [\"Rendering SLS 'base:orchestration.refresh-apache' failed: Conflicting ID 'setup1'\"]\n" +
		"}]");

        Assert.assertFalse(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testValidateDockerInitWithArrays() {
        JSONArray jsonArray = JSONArray.fromObject("[{\n" +
	    "\"data\": {\n" +
	    "\"celery01.mydomain.com\": {\n" +
	    "\"dockerng_|-celery-c-celery_task-1-container_|-c-celery_task-1_|-running\":   {\n" +
		"\"comment\": \"Container 'c-celery_task-1' was replaced. Image changed from 'registry.mydomain.com/celery_task:11d77cc6c1a21d84fd06f2abc982faf940ee392d' to 'registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357'.\",\n" +
		"\"name\": \"c-celery_task-1\",\n" +
		"\"start_time\": \"17:29:56.303184\",\n" +
		"\"result\": true,\n" +
		"\"duration\": 4973.264,\n" +
		"\"__run_num__\": 1,\n" +
		"\"changes\":     {\n" +
		"\"diff\": {\"image\":       {\n" +
		"\"new\": \"registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357\",\n" +
		"\"old\": \"registry.mydomain.com/celery_task:11d77cc6c1a21d84fd06f2abc982faf940ee392d\"\n" +
		"}},\n" +
		"\"removed\": [\"7913e24c53235f2ecf915ca397ea7e48ef9b4f4245b1cb25e72a96bba506314f\"],\n" +
		"\"added\":       {\n" +
		"\"Time_Elapsed\": 0.03751707,\n" +
		"\"Id\": \"77d2301d0e0735550b9cf535fbac466b611be59ac70f351061c00b77395d1151\",\n" +
		"\"Name\": \"c-celery_task-1\",\n" +
		"\"Warnings\": null\n" +
		"}\n" +
		"}\n" +
		"},\n" +
		"\"dockerng_|-celery-celery_task-image_|-registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357_|-image_present\":   {\n" +
		"\"comment\": \"Image 'registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357' was pulled\",\n" +
		"\"name\": \"registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357\",\n" +
		"\"start_time\": \"17:29:48.444762\",\n" +
		"\"result\": true,\n" +
		"\"duration\": 7858.216,\n" +
		"\"__run_num__\": 0,\n" +
		"\"changes\":     {\n" +
		"\"Layers\":       {\n" +
		"\"Pulled\":         [\n" +
		"\"a3ed95caeb02\",\n" +
		"\"8fb0f56b3447\",\n" +
		"\"0d1cc929b218\"\n" +
		"],\n" +
		"\"Already_Pulled\":         [\n" +
		"\"4ff201d9f6ac\",\n" +
		"\"a3ed95caeb02\",\n" +
		"\"b4b8389cb98d\",\n" +
		"\"071ee56cce56\",\n" +
		"\"9690bd523008\",\n" +
		"\"c1c4f1a1eb80\",\n" +
		"\"177a932959c7\"\n" +
		"]\n" +
		"},\n" +
		"\"Status\": \"Downloaded newer image for registry.mydomain.com/celery_task:84ff045f69b25c4143000aa744d771eed7373357\",\n" +
		"\"Time_Elapsed\": 7.8480453\n" +
		"}\n" +
		"}\n" +
		"}}}]");

	Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }
 
    @Test
    public void testValidateFunctionCallForEmptyResponse() {
        JSONArray jsonArray = JSONArray.fromObject("[{}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }
 
    @Test
    public void testValidateFunctionCallForShortEmptyResponse() {
        JSONArray jsonArray = JSONArray.fromObject("[{\n" +
	    "\"minion1\":\"\",\n" +
	    "\"minion2\":\"\"\n" +
	    "}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testValidateFunctionCallForMultiPillar() {
	JSONArray jsonArray = JSONArray.fromObject("[{\"data\":{\"salt_master\":{\n" +
		"\"salt_|-DNS_count_|-cmd.run_|-function\":{\"__run_num__\":1,\"changes\":{\n" +
		"\"out\":\"highstate\",\"ret\":{\"salt\":\"/tmp/count.txt\"}},\n" +
		"\"comment\":\"Functionransuccessfully.Functioncmd.runranonsalt.\",\"duration\":\"216.56ms\",\"name\":\"cmd.run\",\"result\":true,\"start_time\":\"10:14:19.791039\"},\n" +
		"\"salt_|-dns_action_|-dns_action_|-state\":{\"__run_num__\":2,\"changes\":{\"out\":\"highstate\",\"ret\":{\"salt\":{\"cmd_|-create_DNS_record_|-echo\\\"DNSRECORDNOTINUSE-PROCEEDTOUPDATEDNS\\\"_|-run\":{\"__run_num__\":1,\"changes\":{\"pid\":2447,\"retcode\":0,\"stderr\":\"\",\"stdout\":\"DNSRECORDNOTINUSE-PROCEEDTOUPDATEDNS\"},\n" +
		"\"comment\":\"Command\\\"echo\\\"DNSRECORDNOTINUSE-PROCEEDTOUPDATEDNS\\\"\\\"run\",\"duration\":\"4.09ms\",\"name\":\"echo\\\"DNSRECORDNOTINUSE-PROCEEDTOUPDATEDNS\\\"\",\"result\":true,\"start_time\":\"10:14:20.172160\"},\n" +
		"\"ddns_|-add_dns_host_|-adrian_|-present\":{\"__run_num__\":0,\"changes\":{\"data\":\"10.10.10.18\",\"name\":\"adrian\",\"rdtype\":\"A\",\"ttl\":86400,\"zone\":\"saltlab.int.\"},\n" +
		"\"comment\":\"UpdatedArecordfor\\\"adrian\\\"\",\"duration\":\"7.328ms\",\"name\":\"adrian\",\"result\":true,\"start_time\":\"10:14:20.164129\"}}}},\n" +
		"\"comment\":\"Statesransuccessfully.Updatingsalt.\",\"duration\":\"304.499ms\",\"name\":\"dns_action\",\"result\":true,\"start_time\":\"10:14:20.007795\"},\n" +
		"\"salt_|-list_duplicates_|-list_duplicates_|-state\":{\"__run_num__\":0,\"changes\":{\"out\":\"highstate\",\"ret\":{\"salt\":{\"cmd_|-list_duplicate_hosts_|-/srv/salt/prod/dns/find_dups.sh10.10.10.18saltlab.int.192.168.15.4_|-run\":{\"__run_num__\":0,\n" +
		"\"changes\":{\"pid\":2399,\"retcode\":0,\"stderr\":\"\",\"stdout\":\"\"},\n" +
		"\"comment\":\"Command\\\"/srv/salt/prod/dns/find_dups.sh10.10.10.18saltlab.int.192.168.15.4\\\"run\",\"duration\":\"18.112ms\",\"name\":\"/srv/salt/prod/dns/find_dups.sh10.10.10.18saltlab.int.192.168.15.4\",\"result\":true,\"start_time\":\"10:14:19.639361\"}}}},\n" +
		"\"comment\":\"Statesransuccessfully.Updatingsalt.\",\"duration\":\"293.707ms\",\"name\":\"list_duplicates\",\"result\":true,\"start_time\":\"10:14:19.497222\"}}},\n" +
		"\"outputter\":\"highstate\"}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }
    
    @Test
    public void testHighStateChangesTest() {
    JSONArray jsonArray = JSONArray.fromObject("[{" +
    "\"web1\": {" +
        "\"file_|-manage ssh_known_hosts file_|-/etc/ssh/ssh_known_hosts_|-managed\": {" +
            "\"comment\": \"The file /etc/ssh/ssh_known_hosts is set to be changed\"," +
            "\"name\": \"/etc/ssh/ssh_known_hosts\"," +
            "\"start_time\": \"12:32:39.060016\"," +
            "\"result\": null," +
            "\"duration\": 277.921," +
            "\"__run_num__\": 9," +
            "\"changes\": {" +
                "\"diff\": \"diff output\"" +
            "}" +
        "}}}]");

        Assert.assertTrue(Utils.validateFunctionCall(jsonArray));
    }

    @Test
    public void testValidateBusyMinionFails() {
        JSONArray jsonArray = JSONArray.fromObject("[{\n" +
	    "\"outputter\": \"highstate\",\n" +
            "\"data\": {\"p-3-testmap.pio.tv.ro\": [\"The function \\\"state.sls\\\" is running as PID 0 and was started at 1 with jid 2\"]}\n" +
	    "}]");

        Assert.assertFalse(Utils.validateFunctionCall(jsonArray));
    }

    @Mock
    TaskListener listenerMock;

    @Mock
    Run jenkinsBuildMock;

    @Before
    public void setUp() throws Exception {
        jenkinsBuildMock = mock(Run.class);
        listenerMock = mock(TaskListener.class);
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKINGENVVAR", "true");
        //when(jenkinsBuildMock.getBuildVariables()).thenReturn(env);
        when(jenkinsBuildMock.getEnvironment(listenerMock)).thenReturn(new EnvVars(env));
        when(listenerMock.getLogger()).thenReturn(System.out);
    }

    @Test
    public void testParamorizeFoundMatch() throws IOException, InterruptedException {
    	Assert.assertEquals(Utils.paramorize(jenkinsBuildMock, listenerMock, "{{WORKINGENVVAR}}"), "true");
    }

    @Test
    public void testParamorizeMissing() throws IOException, InterruptedException {
    	Assert.assertEquals(Utils.paramorize(jenkinsBuildMock, listenerMock, "{{DOESNOTEXIST}}"), "");
    }

}
