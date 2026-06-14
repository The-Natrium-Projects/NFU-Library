package net.sodiumzh.nfu.object;

import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;

/**
 * A utility that casts self to own subclasses. Used for extendable classes with builder-style setters
 * of which builders may return superclass reference.
 * <p>For example:
 * <p>{@code class Parent {Parent init(){...}}}
 * <p>{@code class Child extends Parent {Child init2(){...}}}
 * <p>and now {@code new Child().init().init2()} doesn't compile because {@code init()} returns a {@code Parent} reference.
 * Then when {@code Parent} implements {@code Upcastable<Parent>},
 * you can use: {@code new Child().init().upcast(Child.class).init2()}.
 */
public interface Upcastable<T> {

    /**
     * Cast self to given class. Provided by {@link Upcastable}.
     */
    @ApiStatus.NonExtendable
    public default <U extends T> U upcast(Class<U> clazz) {
        try {
            return (U) this;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH,
                "NFU Upcastable: trying to cast %s to incompatible class %s.",
                this.getClass().getSimpleName(), clazz.getSimpleName()), e);
        }
    }


}
