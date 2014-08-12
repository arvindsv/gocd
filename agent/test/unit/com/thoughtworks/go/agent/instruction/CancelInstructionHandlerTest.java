package com.thoughtworks.go.agent.instruction;

import com.thoughtworks.go.agent.JobRunner;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CancelInstructionHandlerTest {
    @Test
    public void shouldSayThatItCanHandleCancelJobInstruction() throws Exception {
        AgentRuntimeInfo runtimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("randomhost.test", "127.0.0.1", "some-uuid-1"));
        AgentInstruction notACancelInstruction = new AgentInstruction(false);
        AgentInstruction aCancelInstruction = new AgentInstruction(true);

        CancelInstructionHandler handler = new CancelInstructionHandler();
        assertThat(handler.canHandle(notACancelInstruction, runtimeInfo), is(false));
        assertThat(handler.canHandle(aCancelInstruction, runtimeInfo), is(true));
    }

    @Test
    public void shouldNotFailWhenTryingToHandleInstructionWithRunnerNotSet() throws Exception {
        AgentRuntimeInfo runtimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("randomhost.test", "127.0.0.1", "some-uuid-1"));
        AgentInstruction aCancelInstruction = new AgentInstruction(true);

        CancelInstructionHandler handler = new CancelInstructionHandler();
        handler.handleInstruction(aCancelInstruction, runtimeInfo); // No exception.
    }

    @Test
    public void shouldDelegateHandlingInstructionToRunner() throws Exception {
        JobRunner runner = mock(JobRunner.class);

        AgentRuntimeInfo runtimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("randomhost.test", "127.0.0.1", "some-uuid-1"));
        AgentInstruction aCancelInstruction = new AgentInstruction(true);

        CancelInstructionHandler handler = new CancelInstructionHandler();
        handler.setRunner(runner);
        handler.handleInstruction(aCancelInstruction, runtimeInfo);

        verify(runner).handleInstruction(aCancelInstruction, runtimeInfo);
    }
}