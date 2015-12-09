package com.thoughtworks.go.remote.communication;

import com.google.gson.GsonBuilder;

public interface GoGsonTypeAdapter {
    void initialize(GoAgentServerCommunicationSerialization serialization);
}
