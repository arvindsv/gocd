package com.thoughtworks.go.remote.communication;

import com.google.gson.*;

import java.lang.reflect.Type;

/** Generic serializer and deserializer, which captures the concrete type and the value, so that it can be deserialized.
 * Mostly used with interface classes.
 */
public class GsonTypeAdapterForInterfaceType<T> implements JsonSerializer<T>, JsonDeserializer<T>, GoGsonTypeAdapter {
    private GoAgentServerCommunicationSerialization serialization;

    @Override
    public void initialize(GoAgentServerCommunicationSerialization serialization) {
        this.serialization = serialization;
    }

    @Override
    public JsonElement serialize(T value, Type typeOfValue, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("concreteType", value.getClass().getCanonicalName());
        jsonObject.add("value", serialization.serializeValue(value));
        return jsonObject;
    }

    @Override
    public T deserialize(JsonElement jsonElement, Type typeOfElement, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        return serialization.deserializeToType(jsonObject.get("concreteType"), jsonObject.get("value"));
    }
}
