package net.sodiumzh.nfu.function;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.util.NFUInfoStatics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

public class RegistrableFunction<T, R> implements Function<T, R> {

    private final Function<? super T, ? extends R> function;
    private final Class<T> inputClass;
    private final Class<R> outputClass;
    private final String name;
    private String translationKey = null;
    private Object[] translationArgs = null;

    /**
     * @param inputClass Base class of inputs. If inputs cannot be cast to this, always return null.
     * @param outputClass Base class of outputs.
     * @param name An arbitrary string name. Will be displayed in toString().
     * @param function Raw predicate.
     */
    public RegistrableFunction(Class<T> inputClass, Class<R> outputClass,
                                @Nonnull String name,
                                @Nonnull Function<? super T, ? extends R> function) {
        this.inputClass = inputClass;
        this.outputClass = outputClass;
        this.function = function;
        this.name = name;
    }

    /**
     * Enable a translatable {@link MutableComponent} name. Set key and arguments.
     */
    public RegistrableFunction<T, R> setTranslation(String key, Object... args) {
        this.translationKey = key;
        this.translationArgs = args;
        return this;
    }

    public R apply(T t) {
        return this.inputClass.isAssignableFrom(t.getClass()) ? function.apply(t) : null;
    }

    public Optional<R> applyAndCast(T t, Class<? extends R> outputClass) {
        return Optional.ofNullable(this.apply(t)).filter(r -> outputClass.isAssignableFrom(r.getClass()));
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
        return NFURegistries.FUNCTIONS.getKey(this);
    }

    /**
     * @return An optional of {@code this} cast to the given input and output type generics,
     * or empty if this predicate cannot receive the given class as input.
     */
    public <T1, R1> Optional<RegistrableFunction<T1, R1>> castTypes(Class<T1> inClass, Class<R1> outClass) {
        return this.inputClass.isAssignableFrom(inClass) && this.outputClass.isAssignableFrom(outClass) ?
            Optional.of((RegistrableFunction<T1, R1>) this) : Optional.empty();
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
}
