package net.sodiumzh.nfu.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Label author info of classes/methods/fields from other mods/libraries/games.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Credit {

    public String[] value() default {};

}
