package com.thoughtworks.go.remote.communication;

import com.google.gson.*;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.lang.reflect.Type;

/* Understand how to serialize and deserialize an object of type RemoteInvocationResult. */
public class GsonTypeAdapterForRemoteInvocationResult implements JsonSerializer<RemoteInvocationResult>, JsonDeserializer<RemoteInvocationResult>, GoGsonTypeAdapter {
    private GoAgentServerCommunicationSerialization serialization;

    @Override
    public void initialize(GoAgentServerCommunicationSerialization serialization) {
        this.serialization = serialization;
    }

    @Override
    public JsonElement serialize(RemoteInvocationResult remoteInvocationResult, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();

        object.addProperty("typeOfValue", typeOf(remoteInvocationResult.getValue()));
        object.add("value", serialization.serializeValue(remoteInvocationResult.getValue()));

        object.addProperty("typeOfException", typeOf(remoteInvocationResult.getException()));
        object.add("exception", serialization.serializeValue(remoteInvocationResult.getException()));

        return object;
    }

    @Override
    public RemoteInvocationResult deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        JsonElement typeOfValue = jsonObject.get("typeOfValue");
        JsonElement typeOfException = jsonObject.get("typeOfException");

        if (typeOfValue != null && !typeOfValue.isJsonNull()) {
            return new RemoteInvocationResult(serialization.deserializeToType(typeOfValue, jsonObject.get("value")));
        } else if (typeOfException != null && !typeOfException.isJsonNull()) {
            return new RemoteInvocationResult((Throwable) serialization.deserializeToType(typeOfException, jsonObject.get("exception")));
        }
        return new RemoteInvocationResult(null);
    }

    private String typeOf(Object o) {
        return o == null ? null : o.getClass().getCanonicalName();
    }
}
