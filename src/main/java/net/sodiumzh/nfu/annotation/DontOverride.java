package net.sodiumzh.nfu.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * Labels that the default method is not recommended to override.
 * Continue overriding these methods may cause unexpected errors.
 * @deprecated Use {@link org.jetbrains.annotations.ApiStatus.NonExtendable} instead.
 */
@Target(METHOD)
@Retention(RetentionPolicy.CLASS)
public @ interface DontOverride {

}

