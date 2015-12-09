package com.thoughtworks.go.remote.communication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.remote.work.UnregisteredAgentWork;
import com.thoughtworks.go.remote.work.Work;
import org.junit.Before;
import org.junit.Test;
import org.springframework.remoting.support.RemoteInvocation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class GsonTypeAdapterForInterfaceTypeTest {
    private GsonTypeAdapterForInterfaceType<Work> adapter;

    private static final String SERIALIZED_WITH_CONCRETE_TYPE =
            "{\n" +
            "  \"concreteType\": \"com.thoughtworks.go.remote.work.UnregisteredAgentWork\",\n" +
            "  \"value\": {\n" +
            "    \"message\": \"Invalid agent: the UUID[some-uuid] held by this agent is not registered\",\n" +
            "    \"uuid\": \"some-uuid\"\n" +
            "  }\n" +
            "}";

    @Before
    public void setUp() throws Exception {
        Gson gson = GoAgentServiceCommunicationGsonBuilder.create().create();

        adapter = new GsonTypeAdapterForInterfaceType<>();
        adapter.initialize(new GoAgentServerCommunicationSerialization(gson));
    }

    @Test
    public void shouldSerializeWithConcreteType() throws Exception {
        JsonElement serializedValue = adapter.serialize(new UnregisteredAgentWork("some-uuid"), Work.class, null);

        assertThat(serializedValue, is(new JsonParser().parse(SERIALIZED_WITH_CONCRETE_TYPE)));
    }

    @Test
    public void shouldDeserializeUsingConcreteType() throws Exception {
        Work actualWork = adapter.deserialize(new JsonParser().parse(SERIALIZED_WITH_CONCRETE_TYPE), Work.class, null);
        Work expectedWork = new UnregisteredAgentWork("some-uuid");

        assertThat(actualWork, is(expectedWork));
    }
}