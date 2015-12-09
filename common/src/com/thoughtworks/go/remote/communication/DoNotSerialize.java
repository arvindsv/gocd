package com.thoughtworks.go.remote.communication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* Marker to let gson know that this field should not be serialized. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DoNotSerialize {
}
