package com.thoughtworks.go.remote.communication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.remoting.support.RemoteInvocationResult;

import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GsonTypeAdapterForRemoteInvocationResultTest {
    private GsonTypeAdapterForRemoteInvocationResult adapter;

    private static final String RESULT_WITH_STRING = "{\"typeOfValue\":\"java.lang.String\",\"value\":\"result\",\"typeOfException\":null,\"exception\":null}";

    private static final String RESULT_WITH_EXCEPTION =
            "{\n" +
            "  \"typeOfValue\": null,\n" +
            "  \"value\": null,\n" +
            "  \"typeOfException\": \"java.lang.RuntimeException\",\n" +
            "  \"exception\": {\n" +
            "    \"detailMessage\": \"Ouch\",\n" +
            "    \"stackTrace\": [],\n" +
            "    \"suppressedExceptions\": []\n" +
            "  }\n" +
            "}";

    private static final String RESULT_WITH_OBJECT =
            "{\n" +
            "  \"typeOfValue\": \"com.thoughtworks.go.server.service.AgentRuntimeInfo\",\n" +
            "  \"value\": {\n" +
            "    \"identifier\": {\n" +
            "      \"hostName\": \"host1\",\n" +
            "      \"ipAddress\": \"ip1\",\n" +
            "      \"uuid\": \"uuid1\"\n" +
            "    },\n" +
            "    \"runtimeStatus\": \"Idle\",\n" +
            "    \"buildingInfo\": {\n" +
            "      \"buildingInfo\": \"\",\n" +
            "      \"buildLocator\": \"\"\n" +
            "    },\n" +
            "    \"location\": \"workingdir1\",\n" +
            "    \"usableSpace\": 0,\n" +
            "    \"operatingSystemName\": \"Mac OS X\",\n" +
            "    \"cookie\": \"cookie1\",\n" +
            "    \"agentLauncherVersion\": \"version1\"\n" +
            "  },\n" +
            "  \"typeOfException\": null,\n" +
            "  \"exception\": null\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        Gson gson = GoAgentServiceCommunicationGsonBuilder.create().create();

        adapter = new GsonTypeAdapterForRemoteInvocationResult();
        adapter.initialize(new GoAgentServerCommunicationSerialization(gson));
    }

    @Test
    public void shouldSerializeAResultWithAString() throws Exception {
        assertSerialization(RESULT_WITH_STRING, new RemoteInvocationResult("result"));
    }

    @Test
    public void shouldSerializeAResultWithAnObject() throws Exception {
        AgentRuntimeInfo runtimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("host1", "ip1", "uuid1"), "cookie1", "version1", "workingdir1");

        assertSerialization(RESULT_WITH_OBJECT, new RemoteInvocationResult(runtimeInfo));
    }

    @Test
    public void shouldDeserializeAResultWithAString() throws Exception {
        assertDeserialization(new RemoteInvocationResult("result"), RESULT_WITH_STRING);
    }

    @Test
    public void shouldDeserializeAResultWithAnObject() throws Exception {
        AgentRuntimeInfo runtimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("host1", "ip1", "uuid1"), "cookie1", "version1", "workingdir1");

        assertDeserialization(new RemoteInvocationResult(runtimeInfo), RESULT_WITH_OBJECT);
    }

    @Test
    public void shouldSerializeAResultWithAnException() throws Exception {
        assertSerialization(RESULT_WITH_EXCEPTION, new RemoteInvocationResult(new RuntimeException("Ouch")));
    }

    @Test
    public void shouldDeserializeAResultWithAnException() throws Exception {
        assertDeserializationOfException(new RemoteInvocationResult(new RuntimeException("Ouch")), RESULT_WITH_EXCEPTION);
    }

    /* NOTE: The "exception" part of this is open and any Throwable will be deserialized. Since this is used only on the agent, this
     * is not handled by safe deserialization, for now. To handle it means taking doing a custom serialization for exceptions and
     * serializing only the message and the stacktrace.
     */
    @Test
    public void shouldFailToDeserializeAnInvalidValue() throws Exception {
        String resultWithInvalidType = "{\"typeOfValue\":\"com.thoughtworks.go.domain.Pipeline\",\"value\":\"\",\"typeOfException\":null,\"exception\":null}";
        try {
            deserialize(resultWithInvalidType);
            fail("This should have failed to deserialize since Pipeline class is not in BuildRepositoryRemote and is invalid.");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Invalid class for deserialization: class com.thoughtworks.go.domain.Pipeline"));
        }
    }

    private void assertSerialization(String expectedResult, RemoteInvocationResult valueToSerialize) {
        JsonElement actual = adapter.serialize(valueToSerialize, RemoteInvocationResult.class, null);
        assertThat(actual, is(new JsonParser().parse(expectedResult)));
    }

    private void assertDeserialization(RemoteInvocationResult expectedResult, String valueToDeserialize) {
        RemoteInvocationResult actual = deserialize(valueToDeserialize);

        String message = "\nExpected: " + reflectionToString(expectedResult) + "\nActual: " + reflectionToString(actual);
        assertTrue(message, EqualsBuilder.reflectionEquals(expectedResult, actual));
    }

    private void assertDeserializationOfException(RemoteInvocationResult expectedResult, String serializedValue) {
        RemoteInvocationResult actual = deserialize(serializedValue);

        String message = "\nExpected: " + reflectionToString(expectedResult.getException()) + "\nActual: " + reflectionToString(actual.getException());
        assertTrue(message, EqualsBuilder.reflectionEquals(expectedResult.getException(), actual.getException(), new String[] { "cause" }));
    }

    private RemoteInvocationResult deserialize(String serializedValue) {
        return adapter.deserialize(new JsonParser().parse(serializedValue), RemoteInvocationResult.class, null);
    }
}