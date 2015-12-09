package com.thoughtworks.go.remote.communication;

import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GoAgentServerCommunicationTypeValidationTest {
    private GoAgentServerCommunicationTypeValidation validation;

    @Before
    public void setUp() throws Exception {
        validation = new GoAgentServerCommunicationTypeValidation();
    }

    @Test
    public void shouldConsiderADirectClassInBuildRepositoryRemoteInterfaceAsValid() throws Exception {
        assertTrue(validation.isValid(AgentRuntimeInfo.class));
    }

    @Test
    public void shouldConsiderADescendantOfAClassInBuildRepositoryRemoteInterfaceAsValid() throws Exception {
        assertTrue(validation.isValid(Work.class));
        assertTrue(validation.isValid(BuildWork.class));
    }

    @Test
    public void shouldConsiderAClassWhichIsNotInBuildRepositoryRemoteInterfaceAsInvalid() throws Exception {
        assertFalse(validation.isValid(Pipeline.class));
    }
}