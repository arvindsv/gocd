package com.thoughtworks.go.remote.communication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.thoughtworks.go.domain.DefaultJobPlan;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.work.*;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class GoAgentServerCommunicationSerializationTest {
    private GoAgentServerCommunicationSerialization serialization;

    public static final JsonElement SERIALIZED_VALUE = new JsonParser().parse("{\"message\":\"Invalid agent: the UUID[some-uuid] held by this agent is not registered\",\"uuid\":\"some-uuid\"}");

    @Before
    public void setUp() throws Exception {
        Gson gson = GoAgentServiceCommunicationGsonBuilder.create().create();
        serialization = new GoAgentServerCommunicationSerialization(gson);
    }

    @Test
    public void shouldDeserializeAValueBasedOnAType() throws Exception {
        JsonPrimitive elementForType = new JsonPrimitive(UnregisteredAgentWork.class.getCanonicalName());

        UnregisteredAgentWork actualValue = serialization.deserializeToType(elementForType, SERIALIZED_VALUE);
        assertThat(actualValue, is(new UnregisteredAgentWork("some-uuid")));
    }

    @Test
    public void shouldSerializeAnArbitraryValue() throws Exception {
        Work work = new UnregisteredAgentWork("some-uuid");
        assertThat(serialization.serializeValue(work), is(SERIALIZED_VALUE));
    }

    @Test
    public void ensureThatOnlyTheChosenSetOfClassesIsValidForDeserialization() throws Exception {
        assertSuccessfulDeserialization(new BuildWork(BuildAssignmentMother.start().done()));
        assertSuccessfulDeserialization(new NoWork());
        assertSuccessfulDeserialization(new DeniedAgentWork("uuid"));
        assertSuccessfulDeserialization(new UnregisteredAgentWork("uuid"));

        assertSuccessfulDeserialization(new AgentInstruction(true));
        assertSuccessfulDeserialization(JobIdentifierMother.anyBuildIdentifier());
        assertSuccessfulDeserialization(JobState.Assigned);
        assertSuccessfulDeserialization(JobResult.Failed);
        assertSuccessfulDeserialization(true);
        assertSuccessfulDeserialization("some-string");

        assertSuccessfulDeserialization(MaterialsMother.dependencyMaterial());
        assertSuccessfulDeserialization(JobInstanceMother.jobPlan("job1", 1));

        assertFailedDeserialization(ModificationsMother.multipleModificationsInHg().get(0));
    }

    private void assertSuccessfulDeserialization(Object objectOfAValidTypeForDeserialization) {
        assertSuccessfulDeserialization(objectOfAValidTypeForDeserialization, objectOfAValidTypeForDeserialization.getClass());
    }

    private void assertSuccessfulDeserialization(Object objectOfAValidTypeForDeserialization, Class<?> classToUseToTryToDeserialize) {
        tryRoundtrip(objectOfAValidTypeForDeserialization, classToUseToTryToDeserialize, true);
    }

    private void assertFailedDeserialization(Object objectOfAnInvalidTypeForDeserialization) {
        assertFailedDeserialization(objectOfAnInvalidTypeForDeserialization, objectOfAnInvalidTypeForDeserialization.getClass());
    }

    private void assertFailedDeserialization(Object objectOfAValidTypeForDeserialization, Class<?> classToUseToTryToDeserialize) {
        tryRoundtrip(objectOfAValidTypeForDeserialization, classToUseToTryToDeserialize, false);
    }

    private void tryRoundtrip(Object object, Class<?> classToUseToTryToDeserialize, boolean shouldSucceed) {
        JsonElement serializedValue = serialization.serializeValue(object);
        try {
            serialization.deserializeToType(new JsonPrimitive(classToUseToTryToDeserialize.getCanonicalName()), serializedValue);

            if (!shouldSucceed) {
                fail("Should not have deserialized " + object + " using class: " + classToUseToTryToDeserialize);
            }
        } catch (Exception e) {
            if (shouldSucceed) {
                fail("Should have deserialized " + object + " using class: " + classToUseToTryToDeserialize);
            }
        }
    }
}