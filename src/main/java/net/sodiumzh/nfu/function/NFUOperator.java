package net.sodiumzh.nfu.function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.sodiumzh.nfu.object.NFUJsonParser;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.util.NFUResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An operator receiving a subject and variable arguments read from JSON. NFU-registrable.
 * <p>This class is for data-driven function call, and arguments should be able to get loaded from data.
 * @param <S> Operator subject type.
 * @param <R> Operator result type.
 */
public class NFUOperator<S, R> implements BiFunction<S, Map<String, Object>, R> {

    private final BiFunction<S, Map<String, Object>, R> function;
    private final Map<String, NFUJsonParser<?>> argParserMap;
    private final Class<S> subjectClass;
    private final Class<R> resultClass;

    public NFUOperator(Class<S> subjectClass, Class<R> resultClass, Map<String, NFUJsonParser<?>> argParserMap,
                       BiFunction<S, Map<String, Object>, R> function) {
        this.subjectClass = subjectClass;
        this.resultClass = resultClass;
        this.argParserMap = Map.copyOf(argParserMap);
        this.function = function;
        if (argParserMap.containsKey("operator") || argParserMap.containsKey("function"))
            throw new IllegalArgumentException("\"operator\" are \"function\" are reserved keywords, and cannot be used as an argument name.");
    }

    /**
     * Create a simple function without args.
     */
    public static <S, T> NFUOperator<S, T> simpleFunction(Class<S> subjectClass, Class<T> resultClass, Function<S, T> function) {
        return new NFUOperator<>(subjectClass, resultClass, Map.of(), (s, m) -> function.apply(s));
    }

    /**
     * Create a predicate (boolean-valued function) with varargs.
     */
    public static <S> NFUOperator<S, Boolean> predicate(Class<S> subjectClass, Map<String, NFUJsonParser<?>> argParserMap,
                                                        BiPredicate<S, Map<String, Object>> predicate) {
        return new NFUOperator<>(subjectClass, Boolean.class, argParserMap, predicate::test);
    }

    /**
     * Create a simple predicate without args.
     */
    public static <S> NFUOperator<S, Boolean> simplePredicate(Class<S> subjectClass, Predicate<S> predicate) {
        return new NFUOperator<>(subjectClass, Boolean.class, Map.of(), (s, map) -> predicate.test(s));
    }

    private Map<String, Object> parseArgs(JsonElement data) {
        if (argParserMap.size() == 0) return Map.of();
        // For operators with only one argument, allow to directly use primitive
        if (argParserMap.size() == 1 && data.isJsonPrimitive()) {
            var onlyEntry = argParserMap.entrySet().stream().findAny().get();
            var value = onlyEntry.getValue().parse(data);
            return value != null ? Map.of(onlyEntry.getKey(), value) : Map.of();
        }
        // Otherwise a json object is required
        else if (!data.isJsonObject()) return Map.of();
        Map<String, Object> res = new HashMap<>();
        JsonObject jsonObject = data.getAsJsonObject();
        for (var entry: this.argParserMap.entrySet()) {
            JsonElement entryElem = jsonObject.get(entry.getKey());
            if (entryElem != null) {
                Object value = entry.getValue().parse(entryElem);
                if (value != null) res.put(entry.getKey(), value);
            }
        }
        return res;
    }

    /**
     * Apply the operator with an argument map.
     */
    @Override
    public R apply(S subject, Map<String, Object> args) {
        try {
            if (!this.subjectClass.isAssignableFrom(subject.getClass()))
                throw new IllegalArgumentException("Input subject class " + subject.getClass().getName() + " doesn't match required class " + this.subjectClass.getName() + ".");
            return function.apply(subject, args);
        } catch (RuntimeException e) {
            LogUtils.getLogger().error("Data-driven operator " + this.getRegistryKeyString() + " invocation failed. Return null.", e);
            return null;
        }
    }

    /**
     * Apply the operator with all arguments null. Mainly for invoking simple functions.
     */
    public R applySimple(S subject) {
        return this.apply(subject, Map.of());
    }

    private R applyUnsafe(Object subject, Map<String, Object> args) {
        return this.apply((S)subject, args);
    }

    public R applyByData(S subject, JsonElement element) {
        return this.apply(subject, this.parseArgs(element));
    }

    private R applyByDataUnsafe(Object subject, JsonElement element) {
        return this.applyByData((S) subject, element);
    }

    public boolean isSimpleFunction() {
        return this.argParserMap.isEmpty();
    }

    public Function<S, R> asSimpleFunction() {
        if (this.isSimpleFunction())
            return s -> this.function.apply(s, Map.of());
        else throw new IllegalCallerException("Operator " + NFURegistries.OPERATORS.getOptionalKey(this).map(ResourceLocation::toString).orElse("(MISSING)")
            + " is not a simple function.");
    }

    public boolean isPredicate() {
        return this.resultClass.equals(Boolean.class);
    }

    public BiPredicate<S, Map<String, Object>> asPredicate() {
        if (this.isPredicate())
            return (s, map) -> (Boolean) (this.function.apply(s, map));
        else throw new IllegalCallerException("Operator " + this.getRegistryKeyString()
            + " is not a predicate.");
    }

    public boolean isSimplePredicate() {
        return isSimpleFunction() && isPredicate();
    }

    public Predicate<S> asSimplePredicate() {
        if (this.isSimplePredicate())
            return (s) -> (Boolean) (this.function.apply(s, Map.of()));
        else throw new IllegalCallerException("Operator " + this.getRegistryKeyString()
            + " is not a simple predicate.");
    }

    /**
     * Parse a json element describing an operator and its argument map, and output as a function.
     */
    @Nullable
    public static Function<Object, Object> getOperatorAsFunctionByData(JsonElement json) {
        try {
            if (json.isJsonPrimitive()) {
                ResourceLocation operatorKey = NFUResourceLocation.of(json.getAsString());
                NFUOperator<?, ?> operator = NFURegistries.OPERATORS.getOptionalValue(operatorKey)
                    .orElseThrow(() -> new IllegalArgumentException("Missing operator"));
                return subject -> operator.applyUnsafe(subject, Map.of());
            } else if (json.isJsonObject()) {
                JsonObject jo = json.getAsJsonObject();
                // Get operator
                String operatorKeyString = null;
                if (jo.has("operator"))
                    operatorKeyString = jo.get("operator").getAsString();
                else if (jo.has("function"))
                    operatorKeyString = jo.get("function").getAsString();
                else throw new IllegalArgumentException("Missing operator");
                ResourceLocation operatorKey = NFUResourceLocation.of(operatorKeyString);
                NFUOperator<?, ?> operator = NFURegistries.OPERATORS.getValue(operatorKey);
                if (operator == null) throw new IllegalArgumentException("Missing operator");
                // Then invoke
                return subject -> operator.applyByDataUnsafe(subject, json);
            } else throw new IllegalArgumentException("Illegal json format for operator parse.");
        } catch (RuntimeException e) {
            LogUtils.getLogger().error("Data-driven operator parse failed. Return null.", e);
            return null;
        }
    }

    /**
     * Parse an operator and its argument map from a json, then apply it to a given subject.
     */
    public static Object invokeOperatorByData(Object subject, JsonElement json) {
        Function<Object, Object> function = getOperatorAsFunctionByData(json);
        try {
            return function != null ? function.apply(subject) : null;
        } catch (RuntimeException e) {
            LogUtils.getLogger().error("Data-driven operator invocation failed. Return null.", e);
            return null;
        }
    }

    public Class<S> getSubjectClass() {
        return subjectClass;
    }

    public Class<R> getResultClass() {
        return resultClass;
    }

    private String getRegistryKeyString() {
        return NFURegistries.OPERATORS.getOptionalKey(this).map(ResourceLocation::toString).orElse("(MISSING)");
    }

    /**
     * Cast the subject generic class to the given if compatible. Null if not compatible.
     */
    public <S1> NFUOperator<S1, R> castSubjectClass(Class<S1> castSubjectClass) {
        if (this.subjectClass.isAssignableFrom(castSubjectClass))
            return (NFUOperator<S1, R>)this;
        else return null;
    }

    /**
     * Cast the result generic class to the given if compatible. Null if not compatible.
     */
    public <R1> NFUOperator<S, R1> castResultClass(Class<R1> castResultClass) {
        if (this.resultClass.isAssignableFrom(castResultClass))
            return (NFUOperator<S, R1>)this;
        else return null;
    }

    /**
     * Cast the subject and result generic class to the given if compatible. Null if not compatible.
     */
    public <S1, R1> NFUOperator<S1, R1> castSubjectAndResultClasses(Class<S1> castSubjectClass, Class<R1> castResultClass) {
        if (this.subjectClass.isAssignableFrom(castSubjectClass) && this.resultClass.isAssignableFrom(castResultClass))
            return (NFUOperator<S1, R1>)this;
        else return null;
    }

}
