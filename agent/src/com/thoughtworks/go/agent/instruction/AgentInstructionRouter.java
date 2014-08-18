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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AgentInstructionRouter {
    private final List<AgentInstructionHandler> agentInstructionHandlers = new ArrayList<AgentInstructionHandler>();
    private static final Log LOG = LogFactory.getLog(AgentInstructionRouter.class);

    @Autowired
    public AgentInstructionRouter() {
    }

    public void addHandler(AgentInstructionHandler handler) {
        agentInstructionHandlers.add(handler);
    }

    public void routeInstruction(AgentInstruction instruction, AgentRuntimeInfo agentRuntimeInfo) {
        for (AgentInstructionHandler handler : agentInstructionHandlers) {
            if (handler.canHandle(instruction, agentRuntimeInfo)) {
                handler.handleInstruction(instruction, agentRuntimeInfo);
                return;
            }
        }
        LOG.warn("No handler found for instruction: " + instruction);
    }
}
