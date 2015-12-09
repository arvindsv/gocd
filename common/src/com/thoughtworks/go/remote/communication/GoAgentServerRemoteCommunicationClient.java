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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.thoughtworks.go.remote.communication.GoAgentServiceCommunicationGsonBuilder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.io.IOUtils;
import org.springframework.remoting.httpinvoker.CommonsHttpInvokerRequestExecutor;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* Send a serialized request from the agent to the server. */
public class GoAgentServerRemoteCommunicationClient extends CommonsHttpInvokerRequestExecutor {
    private final Gson gson;

    @Override
    protected void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
        os.write(gson.toJsonTree(invocation).toString().getBytes("UTF-8"));
    }

    @Override
    protected RemoteInvocationResult readRemoteInvocationResult(InputStream is, String codebaseUrl) throws IOException, ClassNotFoundException {
        return gson.fromJson(IOUtils.toString(is, "UTF-8"), RemoteInvocationResult.class);
    }

    public GoAgentServerRemoteCommunicationClient() {
        super();
        this.gson = getGsonFromBuilder();
    }

    public GoAgentServerRemoteCommunicationClient(HttpClient httpClient) {
        super(httpClient);
        this.gson = getGsonFromBuilder();
    }

    private Gson getGsonFromBuilder() {
        GsonBuilder gsonBuilder = GoAgentServiceCommunicationGsonBuilder.create();
        return gsonBuilder.create();
    }
}
