package net.sodiumzh.nfu.function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.sodiumzh.nfu.object.NFUJsonParser;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.util.NFUResourceLocation;

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
public class DataDrivenVarargOperator<S, R> implements BiFunction<S, Map<String, Object>, R> {

    private final BiFunction<S, Map<String, Object>, R> function;
    private final Map<String, NFUJsonParser<?>> argParserMap;
    private final Class<S> subjectClass;
    private final Class<R> resultClass;

    public DataDrivenVarargOperator(Class<S> subjectClass, Class<R> resultClass, Map<String, NFUJsonParser<?>> argParserMap,
                                    BiFunction<S, Map<String, Object>, R> function) {
        this.subjectClass = subjectClass;
        this.resultClass = resultClass;
        this.argParserMap = Map.copyOf(argParserMap);
        this.function = function;
        if (argParserMap.containsKey("operator") || argParserMap.containsKey("function"))
            throw new IllegalArgumentException("\"operator\" are \"function\" are reserved keywords, and cannot be used as an argument name.");
    }

    public static <S, T> DataDrivenVarargOperator<S, T> simpleFunction(Class<S> subjectClass, Class<T> resultClass, Function<S, T> function) {
        return new DataDrivenVarargOperator<>(subjectClass, resultClass, Map.of(), (s, m) -> function.apply(s));
    }

    public static <S> DataDrivenVarargOperator<S, Boolean> predicate(Class<S> subjectClass, Map<String, NFUJsonParser<?>> argParserMap,
                                                                     BiPredicate<S, Map<String, Object>> predicate) {
        return new DataDrivenVarargOperator<>(subjectClass, Boolean.class, argParserMap, predicate::test);
    }

    public static <S> DataDrivenVarargOperator<S, Boolean> simplePredicate(Class<S> subjectClass, Predicate<S> predicate) {
        return new DataDrivenVarargOperator<>(subjectClass, Boolean.class, Map.of(), (s, map) -> predicate.test(s));
    }

    private Map<String, Object> parseArgs(JsonElement data) {
        if (argParserMap.size() == 0) return Map.of();
        // For operators with only one argument, allow to directly use primitive
        if (argParserMap.size() == 1 && data.isJsonPrimitive()) {
            var onlyEntry = argParserMap.entrySet().stream().findAny().get();
            return Map.of(onlyEntry.getKey(), onlyEntry.getValue().parse(data));
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

    public BiPredicate<S, Map<String, Object>> asSimplePredicate() {
        if (this.isSimplePredicate())
            return (s, map) -> (Boolean) (this.function.apply(s, map));
        else throw new IllegalCallerException("Operator " + this.getRegistryKeyString()
            + " is not a simple predicate.");
    }

    public static Object invokeOperatorByData(Object subject, JsonElement json) {
        try {
            if (json.isJsonPrimitive()) {
                ResourceLocation operatorKey = NFUResourceLocation.of(json.getAsString());
                DataDrivenVarargOperator<?, ?> operator = NFURegistries.OPERATORS.getOptionalValue(operatorKey)
                    .orElseThrow(() -> new IllegalArgumentException("Missing operator"));
                return operator.applyUnsafe(subject, Map.of());
            } else if (json.isJsonObject()) {
                JsonObject jo = json.getAsJsonObject();
                String operatorKeyString = jo.get("operator").getAsString();
                if (operatorKeyString == null)
                    operatorKeyString = jo.get("function").getAsString();
                if (operatorKeyString == null)
                    throw new IllegalArgumentException("Missing operator");
                ResourceLocation operatorKey = NFUResourceLocation.of(operatorKeyString);
                DataDrivenVarargOperator<?, ?> operator = NFURegistries.OPERATORS.getValue(operatorKey);
                if (operator == null) throw new IllegalArgumentException("Missing operator");
                return operator.applyByDataUnsafe(subject, json);
            } else throw new IllegalArgumentException("Illegal json format for operator invocation.");
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
}
