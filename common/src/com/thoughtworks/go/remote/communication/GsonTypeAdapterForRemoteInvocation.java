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

import com.google.gson.*;
import com.thoughtworks.go.util.Pair;
import org.springframework.remoting.support.RemoteInvocation;

import java.lang.reflect.Type;

/* Understand how to serialize and deserialize an object of type RemoteInvocation. */
public class GsonTypeAdapterForRemoteInvocation implements JsonSerializer<RemoteInvocation>, JsonDeserializer<RemoteInvocation>, GoGsonTypeAdapter {
    private GoAgentServerCommunicationSerialization serialization;

    @Override
    public void initialize(GoAgentServerCommunicationSerialization serialization) {
        this.serialization = serialization;
    }

    @Override
    public JsonElement serialize(RemoteInvocation remoteInvocation, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("methodName", remoteInvocation.getMethodName());
        jsonObject.add("parameterTypes", toJsonArrayOfClassNames(remoteInvocation.getArguments()));
        jsonObject.add("arguments", serialization.serializeValue(remoteInvocation.getArguments()));
        return jsonObject;
    }

    @Override
    public RemoteInvocation deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        String methodName = jsonObject.getAsJsonPrimitive("methodName").getAsString();
        JsonArray parameterTypes = jsonObject.getAsJsonArray("parameterTypes");
        JsonArray arguments = jsonObject.getAsJsonArray("arguments");

        Pair<Class[], Object[]> pair = getTypesAndTheirArgumentsFrom(parameterTypes, arguments);
        return new RemoteInvocation(methodName, pair.first(), pair.last());
    }

    private Pair<Class[], Object[]> getTypesAndTheirArgumentsFrom(JsonArray parameterTypes, JsonArray arguments) {
        ensureCorrectNumberOfTypesAndArguments(parameterTypes, arguments);
        Class[] classes = new Class[parameterTypes.size()];
        Object[] objects = new Object[arguments.size()];

        for (int i = 0; i < parameterTypes.size(); i++) {
            classes[i] = serialization.toClass(parameterTypes.get(i).getAsJsonPrimitive().getAsString());
            objects[i] = serialization.deserializeToTypeWithTypeValidityCheck(parameterTypes.get(i), arguments.get(i));
        }

        return new Pair<>(classes, objects);
    }

    private void ensureCorrectNumberOfTypesAndArguments(JsonArray parameterTypes, JsonArray arguments) {
        if (parameterTypes.size() != arguments.size()) {
            throw new RuntimeException("JSON is incorrect. Mismatch between number of types and arguments.");
        }
    }

    private JsonElement toJsonArrayOfClassNames(Object[] arguments) {
        JsonArray jsonArrayOfClassNames = new JsonArray();

        for (Object arg : arguments) {
            jsonArrayOfClassNames.add(new JsonPrimitive(arg.getClass().getCanonicalName()));
        }

        return jsonArrayOfClassNames;
    }

}
