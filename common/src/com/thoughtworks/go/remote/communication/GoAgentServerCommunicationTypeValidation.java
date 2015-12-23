package com.thoughtworks.go.remote.communication;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.*;

/* Understands valid classes for top-level of deserialization and ensures that lookup is quick. */
public class GoAgentServerCommunicationTypeValidation {
    private final Set<Class<?>> validClassesForDeserialization;

    public GoAgentServerCommunicationTypeValidation() {
        validClassesForDeserialization = initialListOfValidClassesForDeserialization();
    }

    public boolean isValid(Class clazz) {
        if (validClassesForDeserialization.contains(clazz)) {
            return true;
        }

        if (someClassInHierarchyHasTheAnnotation(clazz)) {
            validClassesForDeserialization.add(clazz);
            return true;
        }

        return false;
    }

    private Set<Class<?>> initialListOfValidClassesForDeserialization() {
        Set<Class<?>> validClasses = new HashSet<>();
        validClasses.add(String.class);
        validClasses.add(Boolean.class);
        validClasses.add(RuntimeException.class); /* TODO: What other exceptions are possible, from Spring? */
        return validClasses;
    }

    private boolean someClassInHierarchyHasTheAnnotation(Class classUnderConsideration) {
        if (classUnderConsideration.isAnnotationPresent(AllowInSerializationBetweenAgentAndServer.class)) {
            return true;
        }

        for (Class anInterface : classUnderConsideration.getInterfaces()) {
            if (someClassInHierarchyHasTheAnnotation(anInterface)) {
                return true;
            }
        }

        Class clazz = classUnderConsideration.getSuperclass();
        while (clazz != null) {
            if (someClassInHierarchyHasTheAnnotation(clazz)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }

        return false;
    }
}
