package com.thoughtworks.go.remote.communication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.UnregisteredAgentWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.springframework.remoting.support.RemoteInvocation;

import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class GsonTypeAdapterForRemoteInvocationTest {
    private GsonTypeAdapterForRemoteInvocation adapter;

    private static final String SIMPLE_INVOCATION = "{\"methodName\":\"methodName\",\"parameterTypes\":[\"java.lang.String\"],\"arguments\":[\"arg1\"]}";

    private static final String INVOCATION_WITH_OBJECTS =
            "{\n" +
            "  \"methodName\": \"methodName\",\n" +
            "  \"parameterTypes\": [\n" +
            "    \"java.lang.String\",\n" +
            "    \"com.thoughtworks.go.server.service.AgentRuntimeInfo\",\n" +
            "    \"com.thoughtworks.go.remote.work.UnregisteredAgentWork\"\n" +
            "  ],\n" +
            "  \"arguments\": [\n" +
            "    \"arg1\",\n" +
            "    {\n" +
            "      \"identifier\": {\n" +
            "        \"hostName\": \"host1\",\n" +
            "        \"ipAddress\": \"ip1\",\n" +
            "        \"uuid\": \"uuid1\"\n" +
            "      },\n" +
            "      \"runtimeStatus\": \"Idle\",\n" +
            "      \"buildingInfo\": {\n" +
            "        \"buildingInfo\": \"\",\n" +
            "        \"buildLocator\": \"\"\n" +
            "      },\n" +
            "      \"location\": \"workingdir1\",\n" +
            "      \"usableSpace\": 0,\n" +
            "      \"operatingSystemName\": \"Mac OS X\",\n" +
            "      \"cookie\": \"cookie1\",\n" +
            "      \"agentLauncherVersion\": \"version1\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"message\": \"Invalid agent: the UUID[some-agent-uuid] held by this agent is not registered\",\n" +
            "      \"uuid\": \"some-agent-uuid\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        Gson gson = GoAgentServiceCommunicationGsonBuilder.create().create();

        adapter = new GsonTypeAdapterForRemoteInvocation();
        adapter.initialize(new GoAgentServerCommunicationSerialization(gson));
    }

    @Test
    public void shouldSerializeASimpleInvocation() throws Exception {
        RemoteInvocation invocation = new RemoteInvocation("methodName", new Class[]{String.class}, new Object[]{"arg1"});
        assertSerialization(SIMPLE_INVOCATION, invocation);
    }

    @Test
    public void shouldSerializeAnInvocationWithMultipleObjects() throws Exception {
        AgentRuntimeInfo runtimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("host1", "ip1", "uuid1"), "cookie1", "version1", "workingdir1");
        Work work = new UnregisteredAgentWork("some-agent-uuid");

        RemoteInvocation invocation = new RemoteInvocation(
                "methodName",
                new Class[]{String.class, AgentRuntimeInfo.class, work.getClass()},
                new Object[]{"arg1", runtimeInfo, work});

        assertSerialization(INVOCATION_WITH_OBJECTS, invocation);
    }

    @Test
    public void shouldDeserializeASimpleInvocation() throws Exception {
        RemoteInvocation invocation = new RemoteInvocation("methodName", new Class[]{String.class}, new Object[]{"arg1"});
        assertDeserialization(invocation, SIMPLE_INVOCATION);
    }

    @Test
    public void shouldDeserializeAnInvocationWithMultipleObjects() throws Exception {
        AgentRuntimeInfo runtimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("host1", "ip1", "uuid1"), "cookie1", "version1", "workingdir1");
        Work work = new UnregisteredAgentWork("some-agent-uuid");

        RemoteInvocation invocation = new RemoteInvocation(
                "methodName",
                new Class[]{String.class, AgentRuntimeInfo.class, work.getClass()},
                new Object[]{"arg1", runtimeInfo, work});

        assertDeserialization(invocation, INVOCATION_WITH_OBJECTS);
    }

    @Test
    public void shouldSerializeAndThenDeserializeEvenIfTheProvidedTypeIsNotConcrete() throws Exception {
        Class<Work> nonConcreteType = Work.class;
        RemoteInvocation invocation = new RemoteInvocation("methodName", new Class[]{nonConcreteType}, new Object[]{new UnregisteredAgentWork("some-agent-uuid")});

        RemoteInvocation actualValueAfterRoundtrip = deserialize(serialize(invocation).toString());

        assertThat(actualValueAfterRoundtrip.getParameterTypes()[0], Is.<Class>is(UnregisteredAgentWork.class));
        assertEquality(invocation.getArguments(), actualValueAfterRoundtrip.getArguments());
        assertEquality(invocation.getMethodName(), actualValueAfterRoundtrip.getMethodName());
    }

    @Test
    public void shouldFailToDeserializeIfTheNumberOfValuesInTypesAndArgumentsDontMatch() throws Exception {
        assertInvalidBecauseOfMismatchInCount("Lesser types than arguments", "{\"methodName\":\"methodName\",\"parameterTypes\":[\"com.thoughtworks.go.remote.work.NoWork\"],\"arguments\":[\"arg1\",{}]}");
        assertInvalidBecauseOfMismatchInCount("More types than arguments", "{\"methodName\":\"methodName\",\"parameterTypes\":[\"java.lang.String\",\"java.lang.String\",\"com.thoughtworks.go.remote.work.NoWork\"],\"arguments\":[\"arg1\",{}]}");
    }

    @Test
    public void shouldFailToDeserializeIfAnUnexcpectedTypeIsProvided() throws Exception {
        String messageWithInvalidType = "{\"methodName\":\"methodName\",\"parameterTypes\":[\"com.thoughtworks.go.domain.Pipeline\"],\"arguments\":[{}]}";
        try {
            deserialize(messageWithInvalidType);
            fail("This should have failed to deserialize since Pipeline class is not in BuildRepositoryRemote and is invalid.");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Invalid class for deserialization: class com.thoughtworks.go.domain.Pipeline"));
        }
    }

    private void assertSerialization(String expectedValue, RemoteInvocation valueToSerialize) {
        assertThat(serialize(valueToSerialize), is(new JsonParser().parse(expectedValue)));
    }

    private void assertDeserialization(RemoteInvocation expectedValue, String valueToDeserialize) {
        assertEquality(expectedValue, deserialize(valueToDeserialize));
    }

    private JsonElement serialize(RemoteInvocation valueToSerialize) {
        return adapter.serialize(valueToSerialize, RemoteInvocation.class, null);
    }

    private RemoteInvocation deserialize(String valueToDeserialize) {
        return adapter.deserialize(new JsonParser().parse(valueToDeserialize), RemoteInvocation.class, null);
    }

    private void assertEquality(Object expectedValue, Object actual) {
        String message = "\nExpected: " + reflectionToString(expectedValue) + "\nActual: " + reflectionToString(actual);
        assertTrue(message, EqualsBuilder.reflectionEquals(expectedValue, actual));
    }

    private void assertInvalidBecauseOfMismatchInCount(String explanation, String valueWhichShouldBeInvalidWhenDeserialized) {
        try {
            deserialize(valueWhichShouldBeInvalidWhenDeserialized);
            fail("This should have failed since it has: " + explanation + " - JSON was: " + valueWhichShouldBeInvalidWhenDeserialized);
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("JSON is incorrect. Mismatch between number of types and arguments."));
        }
    }
}