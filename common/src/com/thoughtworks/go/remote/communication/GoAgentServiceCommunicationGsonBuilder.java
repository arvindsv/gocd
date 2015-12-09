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

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.materials.Material;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class GoAgentServiceCommunicationGsonBuilder {
    private static GsonBuilder builder;

    public static GsonBuilder create() {
        if (builder == null) {
            builder = new GsonBuilder()
                    .setExclusionStrategies(new DoNotSerializeAnnotationExclusionStrategy())
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    .serializeNulls();

            List<GoGsonTypeAdapter> adapters = Arrays.asList(
                    addAdapter(builder, Builder.class, new GsonTypeAdapterForInterfaceType<Builder>()),
                    addAdapter(builder, JobPlan.class, new GsonTypeAdapterForInterfaceType<JobPlan>()),
                    addAdapter(builder, Material.class, new GsonTypeAdapterForInterfaceType<Material>()),
                    addAdapter(builder, MaterialInstance.class, new GsonTypeAdapterForInterfaceType<MaterialInstance>()),
                    addAdapter(builder, RemoteInvocation.class, new GsonTypeAdapterForRemoteInvocation()),
                    addAdapter(builder, RemoteInvocationResult.class, new GsonTypeAdapterForRemoteInvocationResult())
            );


            /* TODO: Is this thread-safe? Is the single "gson" object thread-safe? */
            GoAgentServerCommunicationSerialization serialization = new GoAgentServerCommunicationSerialization(builder.create());

            for (GoGsonTypeAdapter adapter : adapters) {
                adapter.initialize(serialization);
            }
        }
        return builder;
    }

    private static GoGsonTypeAdapter addAdapter(GsonBuilder builder, Type type, GoGsonTypeAdapter adapter) {
        builder.registerTypeAdapter(type, adapter);
        return adapter;
    }
}
