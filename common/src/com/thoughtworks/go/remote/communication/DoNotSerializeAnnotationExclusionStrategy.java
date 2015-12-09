package com.thoughtworks.go.remote.communication;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class DoNotSerializeAnnotationExclusionStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return fieldAttributes.getAnnotation(DoNotSerialize.class) != null;
    }

    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
        return false;
    }
}

