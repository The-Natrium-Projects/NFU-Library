package net.sodiumzh.nfu.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * Marks that the method is generally internal and should not be called as API.
 */
@Target(METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface DontCallManually {

}
