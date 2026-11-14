package net.sodiumzh.nfu.function;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.sodiumzh.nfu.registry.NFURegistries;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An operator receiving a subject and variable arguments read from JSON. NFU-registrable.
 * <p>This class is for data-driven function call, and arguments should be able to get loaded from data.
 * @param <T> Operator subject type.
 * @param <R> Operator result type.
 */
public class NFUOperator<T, R> implements Function<T, R> {

    private final Function<T, R> function;
    private final Class<T> paramClass;
    private final Class<R> resultClass;

    public NFUOperator(Class<T> paramClass, Class<R> resultClass, Function<T, R> function) {
        this.paramClass = paramClass;
        this.resultClass = resultClass;
        this.function = function;
    }

    /**
     * Apply the operator with an argument map.
     */
    @Override
    public R apply(T subject) {
        return function.apply(subject);
    }

    public boolean isPredicate() {
        return this.resultClass.equals(Boolean.class);
    }

    public Predicate<T> asPredicate() {
        if (this.isPredicate())
            return (t) -> (Boolean) (this.function.apply(t));
        else throw new IllegalCallerException("Operator " + this.getRegistryKeyString()
            + " is not a predicate.");
    }

    private String getRegistryKeyString() {
        return NFURegistries.OPERATORS.getOptionalKey(this).map(ResourceLocation::toString).orElse("(MISSING)");
    }

    /**
     * Cast the subject generic class to the given if compatible. Null if not compatible.
     */
    @Nullable
    public <S1> NFUOperator<S1, R> castParamClass(Class<S1> castParamClass) {
        if (this.paramClass.isAssignableFrom(castParamClass))
            return (NFUOperator<S1, R>)this;
        else return null;
    }

    /**
     * Cast the result generic class to the given if compatible. Null if not compatible.
     */
    @Nullable
    public <R1> NFUOperator<T, R1> castReturnClass(Class<R1> castReturnClass) {
        if (this.resultClass.isAssignableFrom(castReturnClass))
            return (NFUOperator<T, R1>)this;
        else return null;
    }

    /**
     * Cast the subject and result generic class to the given if compatible. Null if not compatible.
     */
    @Nullable
    public <S1, R1> NFUOperator<S1, R1> castParamAndReturnClasses(Class<S1> castParamClass, Class<R1> castReturnClass) {
        if (this.paramClass.isAssignableFrom(castParamClass) && this.resultClass.isAssignableFrom(castReturnClass))
            return (NFUOperator<S1, R1>)this;
        else return null;
    }

}
