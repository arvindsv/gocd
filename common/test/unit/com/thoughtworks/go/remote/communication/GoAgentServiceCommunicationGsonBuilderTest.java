/* ************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.remote.communication;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.RunIfConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.helper.BuildAssignmentMother;
import com.thoughtworks.go.helper.JobIdentifierMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.DeniedAgentWork;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.UnregisteredAgentWork;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.studios.shine.io.StringOutputStream;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.io.File;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.junit.Assert.assertTrue;

public class GoAgentServiceCommunicationGsonBuilderTest {
    @Test
    public void shouldSendPingAndGetResponse() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");

        assertTransfer(remoteInvocation("ping", agentRuntimeInfo), new RemoteInvocationResult(new AgentInstruction(true)));
    }

    @Test
    public void shouldSendGetWorkAndGetResponseOf_NoWork() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");

        assertTransfer(remoteInvocation("getWork", agentRuntimeInfo), new RemoteInvocationResult(new NoWork()));
    }

    @Test
    public void shouldSendGetWorkAndGetResponseOf_BuildWork_WithCommandBuilder() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");

        CommandBuilder cancelBuilder = new CommandBuilder("cmd1", "args1", new File("workingdir1"), new RunIfConfigs(), null, "desc1", "error1");
        Builder builder = new CommandBuilder("cmd1", "args1", new File("workingdir1"), new RunIfConfigs(RunIfConfig.ANY), cancelBuilder, "desc1", "error1");

        BuildCause buildCause = BuildCause.createWithModifications(ModificationsMother.multipleModifications(), "approver");
        BuildWork work = new BuildWork(BuildAssignmentMother.start().addBuilder(builder).withBuildCause(buildCause).done());

        assertTransfer(remoteInvocation("getWork", agentRuntimeInfo), new RemoteInvocationResult(work));
    }

    @Test
    public void shouldSendGetWorkAndGetResponseOf_BuildWork_WhichHasAMaterialInstanceInModification() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");

        Builder builder = new CommandBuilder("cmd1", "args1", new File("workingdir1"), new RunIfConfigs(RunIfConfig.ANY), null, "desc1", "error1");

        Modification modification = ModificationsMother.oneModifiedFile("rev1");
        modification.setMaterialInstance(new GitMaterialInstance("abcurl", "master", "sub", "flyweight"));
        MaterialRevisions revisions = ModificationsMother.multipleRevisions(MaterialsMother.gitMaterial("abcurl"), 10, modification);

        BuildCause buildCause = BuildCause.createWithModifications(revisions, "approver");
        BuildWork work = new BuildWork(BuildAssignmentMother.start().addBuilder(builder).withBuildCause(buildCause).done());

        assertTransfer(remoteInvocation("getWork", agentRuntimeInfo), new RemoteInvocationResult(work));
    }

    @Test
    public void shouldSendGetWorkAndGetResponseOf_UnregisteredAgentWork() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");

        assertTransfer(remoteInvocation("getWork", agentRuntimeInfo), new RemoteInvocationResult(new UnregisteredAgentWork("invalid-uuid")));
    }

    @Test
    public void shouldSendGetWorkAndGetResponseOf_DeniedAgentWork() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");

        assertTransfer(remoteInvocation("getWork", agentRuntimeInfo), new RemoteInvocationResult(new DeniedAgentWork("agent-uuid")));
    }

    @Test
    public void shouldSendReportCurrentStatusAndGetNoResponse() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();
        JobState jobState = JobState.Scheduled;

        assertTransfer(remoteInvocation("reportCurrentStatus", agentRuntimeInfo, jobIdentifier, jobState), new RemoteInvocationResult(null));
    }

    @Test
    public void shouldSendReportCompletingAndGetNoResponse() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();

        assertTransfer(remoteInvocation("reportCompleting", agentRuntimeInfo, jobIdentifier, JobResult.Failed), new RemoteInvocationResult(null));
    }

    @Test
    public void shouldSendReportCompletedAndGetNoResponse() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();

        assertTransfer(remoteInvocation("reportCompleted", agentRuntimeInfo, jobIdentifier, JobResult.Failed), new RemoteInvocationResult(null));
    }

    @Test
    public void shouldSendIsIgnoredAndGetABooleanResponse() throws Exception {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();

        assertTransfer(remoteInvocation("isIgnored", jobIdentifier), new RemoteInvocationResult(false));
    }

    @Test
    public void shouldSendGetCookieAndGetAStringResponse() throws Exception {
        AgentIdentifier agentIdentifier = new AgentIdentifier("host1", "127.0.0.1", "uuid1");

        assertTransfer(remoteInvocation("getCookie", agentIdentifier, "/path/to/agent1"), new RemoteInvocationResult("cookie1"));
    }

    private void assertTransfer(RemoteInvocation callFromAgentToServer, RemoteInvocationResult responseFromServer) throws Exception {
        ensureMethodExists(responseFromServer.getValue(), callFromAgentToServer.getMethodName(), callFromAgentToServer.getArguments());

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding("UTF-8");

        GoAgentServerRemoteCommunicationClient agentToServer = new GoAgentServerRemoteCommunicationClient();
        GoAgentServerRemoteCommunicationServer serverToAgent = new GoAgentServerRemoteCommunicationServer();


        StringOutputStream stringRepresentationOfRequest = new StringOutputStream();
        agentToServer.writeRemoteInvocation(callFromAgentToServer, stringRepresentationOfRequest);
        RemoteInvocation callReceivedOnServer = serverToAgent.readRemoteInvocation(null, new StringInputStream(stringRepresentationOfRequest.contents()));

        StringOutputStream stringRepresentationOfResponse = new StringOutputStream();
        serverToAgent.writeRemoteInvocationResult(null, response, responseFromServer, stringRepresentationOfResponse);
        RemoteInvocationResult responseRecievedOnAgent = agentToServer.readRemoteInvocationResult(new StringInputStream(stringRepresentationOfResponse.contents()), "unused");

        assertEquality("Request from agent to server", callFromAgentToServer, callReceivedOnServer);
        assertEquality("Response from server to agent", responseFromServer, responseRecievedOnAgent);
    }

    private void ensureMethodExists(Object expectedReturnValueFromServer, String methodName, Object... arguments) throws NoSuchMethodException {
        Class<?>[] typesOfArguments = getTypesOfArguments(arguments);

        Class<?> expectedReturnValueType = expectedReturnValueFromServer == null ? Void.class : expectedReturnValueFromServer.getClass();
        String message = MessageFormat.format("Check if there''s a method with signature: {0} {1}({2}), in {3}.",
                expectedReturnValueType, methodName, join(typesOfArguments, ", "), BuildRepositoryRemote.class.getSimpleName());

        Method method = BuildRepositoryRemote.class.getDeclaredMethod(methodName, typesOfArguments);
        assertTrue(message, method.getReturnType().getName().equals("void") || method.getReturnType().isAssignableFrom(expectedReturnValueType));
    }

    private void assertEquality(String prefixOfMessage, Object expected, Object actual) {
        String message = prefixOfMessage + ":\n" +
                "Expected: " + reflectionToString(expected) + "\n" +
                "  Actual: " + reflectionToString(actual);

        assertTrue(message, EqualsBuilder.reflectionEquals(expected, actual));
    }

    private Class<?>[] getTypesOfArguments(Object[] arguments) {
        List<Class<?>> classes = new ArrayList<>();
        for (Object argument : arguments) {
            classes.add(argument.getClass());
        }
        return classes.toArray(new Class<?>[classes.size()]);
    }

    private RemoteInvocation remoteInvocation(String methodName, Object... arguments) throws Exception {
        return new RemoteInvocation(methodName, getTypesOfArguments(arguments), arguments);
    }
}