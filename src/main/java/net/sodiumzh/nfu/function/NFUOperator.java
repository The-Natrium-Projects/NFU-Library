package net.sodiumzh.nfu.function;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.sodiumzh.nfu.registry.NFURegistries;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An operator receiving a subject and variable arguments read from JSON. NFU-registrable.
 * <p>This class is for data-driven function call, and arguments should be able to get loaded from data.
 * @param <S> Operator subject type.
 * @param <R> Operator result type.
 */
public class NFUOperator<S, R> implements Function<S, R> {

    private final Function<S, R> function;
    private final Class<S> paramClass;
    private final Class<R> returnClass;

    public NFUOperator(Class<S> paramClass, Class<R> resultClass, Function<S, R> function) {
        this.paramClass = paramClass;
        this.returnClass = resultClass;
        this.function = function;
    }

    /**
     * Apply the operator with an argument map.
     */
    @Override
    public R apply(S subject) {
        try {
            if (!this.paramClass.isAssignableFrom(subject.getClass()))
                throw new IllegalArgumentException("Input subject class " + subject.getClass().getName() + " doesn't match required class " + this.paramClass.getName() + ".");
            return function.apply(subject);
        } catch (RuntimeException e) {
            LogUtils.getLogger().error("Data-driven operator " + this.getRegistryKeyString() + " invocation failed. Return null.", e);
            return null;
        }
    }

    public boolean isPredicate() {
        return this.returnClass.equals(Boolean.class);
    }

    public Predicate<S> asPredicate() {
        if (this.isPredicate())
            return (s) -> (Boolean) (this.function.apply(s));
        else throw new IllegalCallerException("Operator " + this.getRegistryKeyString()
            + " is not a predicate.");
    }

    public Class<S> getParamClass() {
        return paramClass;
    }

    public Class<R> getReturnClass() {
        return returnClass;
    }

    private String getRegistryKeyString() {
        return NFURegistries.OPERATORS.getOptionalKey(this).map(ResourceLocation::toString).orElse("(MISSING)");
    }

    /**
     * Cast the subject generic class to the given if compatible. Null if not compatible.
     */
    public <S1> NFUOperator<S1, R> castParamClass(Class<S1> castParamClass) {
        if (this.paramClass.isAssignableFrom(castParamClass))
            return (NFUOperator<S1, R>)this;
        else return null;
    }

    /**
     * Cast the result generic class to the given if compatible. Null if not compatible.
     */
    public <R1> NFUOperator<S, R1> castReturnClass(Class<R1> castReturnClass) {
        if (this.returnClass.isAssignableFrom(castReturnClass))
            return (NFUOperator<S, R1>)this;
        else return null;
    }

    /**
     * Cast the subject and result generic class to the given if compatible. Null if not compatible.
     */
    public <S1, R1> NFUOperator<S1, R1> castParamAndReturnClasses(Class<S1> castParamClass, Class<R1> castReturnClass) {
        if (this.paramClass.isAssignableFrom(castParamClass) && this.returnClass.isAssignableFrom(castReturnClass))
            return (NFUOperator<S1, R1>)this;
        else return null;
    }

}
