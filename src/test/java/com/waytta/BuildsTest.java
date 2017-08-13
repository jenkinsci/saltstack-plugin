package com.waytta;

import static org.junit.Assert.assertEquals;

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

        assertEquals(expectedResult, saltFunc);
    }

    @Test
    public void testMultipleEquals() {
        String myarguments = "thing thing2 keyword=value=thing \"arg with space\"";

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
                + "\"arg\":[\"thing\", \"thing2\", \"arg with space\"],"
                + "\"kwarg\":{\"keyword\":\"value=thing\"},"
                + "}");

        assertEquals(expectedResult, saltFunc);
    }

    @Test
    public void testArgEquals() {
        String myarguments = "'export test=yep; echo $test'";

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
                + "\"arg\":\"export test=yep; echo $test\","
                + "\"kwarg\":{},"
                + "}");

        assertEquals(expectedResult, saltFunc);
    }

    @Test
    public void testIntArgument() {
        String myarguments = "13";

        JSONObject saltFunc = new JSONObject();
        saltFunc.put("client", "local");
        saltFunc.put("tgt", "testTarget");
        saltFunc.put("expr_form", "glob");
        saltFunc.put("fun", "test.rand_sleep");

        Builds.addArgumentsToSaltFunction(myarguments, saltFunc);

        JSONObject expectedResult = JSONObject.fromObject("{\"client\":\"local\","
                + "\"tgt\":\"testTarget\","
                + "\"expr_form\":\"glob\","
                + "\"fun\":\"test.rand_sleep\","
                + "\"arg\": 13,"
                + "\"kwarg\":{},"
                + "}");

        assertEquals(expectedResult, saltFunc);
    }

    @Test
    public void testIntKWArgument() {
        String myarguments = "max=13";

        JSONObject saltFunc = new JSONObject();
        saltFunc.put("client", "local");
        saltFunc.put("tgt", "testTarget");
        saltFunc.put("expr_form", "glob");
        saltFunc.put("fun", "test.rand_sleep");

        Builds.addArgumentsToSaltFunction(myarguments, saltFunc);

        JSONObject expectedResult = JSONObject.fromObject("{\"client\":\"local\","
                + "\"tgt\":\"testTarget\","
                + "\"expr_form\":\"glob\","
                + "\"fun\":\"test.rand_sleep\","
                + "\"kwarg\":{\"max\": 13},"
                + "}");

        assertEquals(expectedResult, saltFunc);
    }

}