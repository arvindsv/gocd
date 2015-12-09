package com.thoughtworks.go.remote.communication;

import com.thoughtworks.go.remote.BuildRepositoryRemote;

import java.lang.reflect.Method;
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

        if (findAssignableClass(clazz)) {
            validClassesForDeserialization.add(clazz);
            return true;
        }

        return false;
    }

    private Set<Class<?>> initialListOfValidClassesForDeserialization() {
        Set<Class<?>> validClasses = new HashSet<>();
        Method[] methodsInInterfaceForCommunication = BuildRepositoryRemote.class.getDeclaredMethods();

        for (Method method : methodsInInterfaceForCommunication) {
            validClasses.add(method.getReturnType());
            Collections.addAll(validClasses, method.getParameterTypes());
        }

        return validClasses;
    }

    private boolean findAssignableClass(Class classUnderConsideration) {
        for (Class<?> validClass : validClassesForDeserialization) {
            if (validClass.isAssignableFrom(classUnderConsideration)) {
                return true;
            }
        }
        return false;
    }
}
