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

import com.thoughtworks.go.agent.JobRunner;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.AgentInstructionTypes;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;

public class CancelInstructionHandler implements AgentInstructionHandler {
    private JobRunner runner;

    public void setRunner(JobRunner runner) {
        this.runner = runner;
    }

    @Override
    public void handleInstruction(AgentInstruction instruction, AgentRuntimeInfo agentRuntimeInfo) {
        if (runner != null) {
            runner.handleInstruction(instruction, agentRuntimeInfo);
        }
    }

    @Override
    public boolean canHandle(AgentInstruction instruction, AgentRuntimeInfo agentRuntimeInfo) {
        return AgentInstructionTypes.TYPE_CANCEL_JOB.equals(instruction.type());
    }
}
