package com.waytta;

import org.junit.Assert;
import org.junit.Test;

import net.sf.json.JSONObject;

public class BuildsTest {
	@Test
    public void testAddArguments() {
		String myarguments = "\"ls -la\" test=True pillar='{\"key\": \"value\"}'";
		
		JSONObject saltFunc = new JSONObject();
		saltFunc.put("client", "local");
		saltFunc.put("tgt", "testTarget");
		saltFunc.put("expr_form", "glob");
		saltFunc.put("fun", "cmd.run");

		Builds.addArgumentsToSaltFunction(myarguments, saltFunc);

		JSONObject expectedResult = JSONObject.fromObject("{\"client\":\"local\","
				+ "\"tgt\":\"testTarget\","
				+ "\"expr_form\":\"glob\","
				+ "\"fun\":\"cmd.run\","
				+ "\"arg\":\"ls -la\","
				+ "\"kwarg\":{"
				+	 "\"test\":\"True\","
				+	 "\"pillar\": {"
				+ 		"\"key\":\"value\""
				+ 	 "}"
				+ "}}");

        Assert.assertTrue(expectedResult.equals(saltFunc));
    }
}