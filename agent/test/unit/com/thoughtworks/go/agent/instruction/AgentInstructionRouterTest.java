package com.thoughtworks.go.agent.instruction;

import com.thoughtworks.go.agent.common.util.LoggingHelper;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class AgentInstructionRouterTest {
    @Test
    public void shouldRouteInstructionToAHandlerWhichClaimsToBeAbleToHandleIt() throws Exception {
        AgentInstruction instruction = new AgentInstruction("TEST_1", "DATA_1");
        AgentRuntimeInfo someRuntimeInfo = mock(AgentRuntimeInfo.class);

        AgentInstructionHandler handler = mock(AgentInstructionHandler.class);
        when(handler.canHandle(instruction, someRuntimeInfo)).thenReturn(true);

        AgentInstructionRouter router = new AgentInstructionRouter();
        router.addHandler(handler);

        router.routeInstruction(instruction, someRuntimeInfo);

        verify(handler).canHandle(instruction, someRuntimeInfo);
        verify(handler).handleInstruction(instruction, someRuntimeInfo);
    }

    @Test
    public void shouldNotFailIfNoHandlerExists() throws Exception {
        AgentInstruction instruction = new AgentInstruction("TEST_1", "DATA_1");
        AgentRuntimeInfo someRuntimeInfo = mock(AgentRuntimeInfo.class);

        AgentInstructionRouter router = new AgentInstructionRouter();
        router.routeInstruction(instruction, someRuntimeInfo);
    }

    @Test
    public void shouldNotFailIfNoHandlerCanHandleInstruction() throws Exception {
        AgentInstruction instruction = new AgentInstruction("TEST_1", "DATA_1");
        AgentRuntimeInfo someRuntimeInfo = mock(AgentRuntimeInfo.class);

        AgentInstructionHandler handler = mock(AgentInstructionHandler.class);
        when(handler.canHandle(instruction, someRuntimeInfo)).thenReturn(true);

        AgentInstructionRouter router = new AgentInstructionRouter();
        router.addHandler(handler);

        AgentInstruction someInstructionWhichIsNotExpected = new AgentInstruction("TEST_2", "DATA_2");
        router.routeInstruction(someInstructionWhichIsNotExpected, someRuntimeInfo);

        verify(handler).canHandle(someInstructionWhichIsNotExpected, someRuntimeInfo);
        verify(handler, times(0)).handleInstruction(someInstructionWhichIsNotExpected, someRuntimeInfo);
    }
}