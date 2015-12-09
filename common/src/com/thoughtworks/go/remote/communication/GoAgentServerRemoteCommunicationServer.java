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
import org.apache.commons.io.IOUtils;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* Expect a serialized request from the agent. */
public class GoAgentServerRemoteCommunicationServer extends org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter {
    public static final String CONTENT_TYPE = "application/vnd.go.cd.v1+json; charset=utf-8";
    private final Gson gson;

    public GoAgentServerRemoteCommunicationServer() {
        GsonBuilder gsonBuilder = GoAgentServiceCommunicationGsonBuilder.create();
        this.gson = gsonBuilder.create();
    }

    @Override
    protected RemoteInvocation readRemoteInvocation(HttpServletRequest request, InputStream is) throws IOException, ClassNotFoundException {
        return gson.fromJson(IOUtils.toString(is, "UTF-8"), RemoteInvocation.class);
    }

    @Override
    protected void writeRemoteInvocationResult(HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result) throws IOException {
        ServletOutputStream outputStream = response.getOutputStream();
        try {
            writeRemoteInvocationResult(request, response, result, outputStream);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    @Override
    protected void writeRemoteInvocationResult(HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result, OutputStream os) throws IOException {
        response.setContentType(CONTENT_TYPE);
        os.write(gson.toJsonTree(result).toString().getBytes("UTF-8"));
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }
}
