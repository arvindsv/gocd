/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.agent.instruction;

import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.LogFixture;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AgentInstructionRouterTest {
    private LogFixture logFixture;

    @Before
    public void setUp() throws Exception {
        logFixture = LogFixture.startListening(Level.WARN);
    }

    @After
    public void tearDown() throws Exception {
        logFixture.stopListening();
    }

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

        assertThat(Arrays.asList(logFixture.getMessages()), hasItem("No handler found for instruction: " + instruction));
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

        assertThat(Arrays.asList(logFixture.getMessages()), hasItem("No handler found for instruction: " + someInstructionWhichIsNotExpected));
        verify(handler).canHandle(someInstructionWhichIsNotExpected, someRuntimeInfo);
        verify(handler, times(0)).handleInstruction(someInstructionWhichIsNotExpected, someRuntimeInfo);
    }
}