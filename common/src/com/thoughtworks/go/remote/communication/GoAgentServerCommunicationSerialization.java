package com.thoughtworks.go.remote.communication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/* Understands serialization of an arbitrary value to JSON and orchestrates safe deserialization of allowed types. */
public class GoAgentServerCommunicationSerialization {
    private final GoAgentServerCommunicationTypeValidation typeValidationForDeserialization;
    private Gson gson;

    public GoAgentServerCommunicationSerialization(Gson gson) {
        this.gson = gson;
        this.typeValidationForDeserialization = new GoAgentServerCommunicationTypeValidation();
    }

    public <T> T deserializeToType(JsonElement typeOfValue, JsonElement value) {
        String typeAsString = typeOfValue.getAsJsonPrimitive().getAsString();
        Class clazz = toClass(typeAsString);
        ensureValidClass(clazz);

        JsonElement jsonElementOfValue = isAPrimitive(clazz) ? value.getAsJsonPrimitive() : value.getAsJsonObject();
        return (T) gson.fromJson(jsonElementOfValue, clazz);
    }

    public Class toClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize java.lang.Class with name: " + className);
        }
    }

    public JsonElement serializeValue(Object value) {
        return gson.toJsonTree(value);
    }

    private void ensureValidClass(Class clazz) {
        if (!typeValidationForDeserialization.isValid(clazz)) {
            throw new RuntimeException("Invalid class for deserialization: " + clazz);
        }
    }

    private boolean isAPrimitive(Class clazz) {
        return String.class.isAssignableFrom(clazz) || Boolean.class.isAssignableFrom(clazz) || Enum.class.isAssignableFrom(clazz);
    }
}
