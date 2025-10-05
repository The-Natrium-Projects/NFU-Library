package net.sodiumzh.nfu.function;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.util.NFUInfoStatics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A {@link Predicate} with some meta info for NFU Predicate registry.
 */
public class RegistrablePredicate<T> implements Predicate<T> {

    private final Predicate<? super T> predicate;
    private final Class<T> inputClass;
    private final String name;
    private String translationKey = null;
    private Object[] translationArgs = null;

    /**
     * @param inputClass Base class of inputs. If inputs cannot be cast to this, always return false.
     * @param name An arbitrary string name. Will be displayed in toString().
     * @param predicate Raw predicate.
     */
    public RegistrablePredicate(Class<T> inputClass, @Nonnull String name, @Nonnull Predicate<? super T> predicate) {
        this.inputClass = inputClass;
        this.predicate = predicate;
        this.name = name;
    }

    /**
     * Enable a translatable {@link MutableComponent} name. Set key and arguments.
     */
    public RegistrablePredicate<T> setTranslation(String key, Object... args) {
        this.translationKey = key;
        this.translationArgs = args;
        return this;
    }

    public boolean test(T t) {
        return this.inputClass.isAssignableFrom(t.getClass()) && predicate.test(t);
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nullable
    public MutableComponent getTranslation() {
        if (translationKey != null)
            return NFUInfoStatics.createTranslatable(translationKey, translationArgs == null ? new Object[]{} : translationArgs);
        else return null;
    }

    @Nullable
    public ResourceLocation getRegistryKey() {
        return NFURegistries.PREDICATES.getKey(this);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("RegistrablePredicate {name = '").append(name) .append("'");
        ResourceLocation regKey = this.getRegistryKey();
        if (regKey != null)
            s.append(", registryKey = '").append(regKey).append("'");
        if (translationKey != null)
            s.append(", translationKey = '").append(translationKey).append("'");
        return s.append("}").toString();
    }

    /**
     * @return An optional of {@code this} cast to the given input type generics, or empty if this predicate cannot
     * receive the given class as input.
     */
    public <R> Optional<RegistrablePredicate<R>> castInputType(Class<R> inClass) {
        return this.inputClass.isAssignableFrom(inClass) ? Optional.of((RegistrablePredicate<R>)this) : Optional.empty();
    }

    public static <T> RegistrablePredicate<T> and(Class<T> inputClass, String name, Predicate<? super T> a, Predicate<? super T> b) {
        return new RegistrablePredicate<>(inputClass, name, (T t) -> a.test(t) && b.test(t));
    }

    public static <T> RegistrablePredicate<T> or(Class<T> inputClass, String name, Predicate<? super T> a, Predicate<? super T> b) {
        return new RegistrablePredicate<>(inputClass, name, (T t) -> a.test(t) || b.test(t));
    }

    public static <T> RegistrablePredicate<T> not(Class<T> inputClass, String name, Predicate<? super T> a) {
        return new RegistrablePredicate<>(inputClass, name, (T t) -> !a.test(t));
    }

    public static <T> RegistrablePredicate<T> xor(Class<T> inputClass, String name, Predicate<? super T> a, Predicate<? super T> b) {
        return new RegistrablePredicate<>(inputClass, name, (T t) -> a.test(t) != b.test(t));
    }

    /**
     * Cast self to predicate of a context-referred subclass.
     */
    @SuppressWarnings("unchecked")
    public <U extends T> RegistrablePredicate<U> cast() {
        return (RegistrablePredicate<U>)this;
    }

}
