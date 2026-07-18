package net.sodiumzh.nfu.entity.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates this method should never be called inside {@link CEntityComponentManager} constructor,
 * including {@link EntityComponentSetupEvent} and {@link EntityComponentFinalizeSetupEvent} listeners.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotAvailableInManagerConstruction {
}
