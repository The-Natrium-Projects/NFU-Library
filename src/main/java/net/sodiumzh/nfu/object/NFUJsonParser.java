package net.sodiumzh.nfu.object;

import com.google.gson.JsonElement;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.registry.NFURegistry;
import net.sodiumzh.nfu.registry.NFURegistryEntryCollection;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

/**
 * Describing a type that can be deserializer from JSON.
 * NFU-registrable.
 */
public class NFUJsonParser<T> {

    private final Class<T> objClass;
    private final Function<JsonElement, T> parser;
    private final T fallback;

    @ApiStatus.Internal
    public static final NFURegistryEntryCollection<NFUJsonParser<?>> REG =
        NFURegistryEntryCollection.create(NFURegistries.JSON_PARSERS, NFULibrary.MOD_ID);
    public static final NFURegistry.Accessor<NFUJsonParser<Boolean>> BOOLEAN =
        REG.register("boolean", () -> new NFUJsonParser<>(Boolean.class, JsonElement::getAsBoolean, false));
    public static final NFURegistry.Accessor<NFUJsonParser<Integer>> INTEGER =
        REG.register("integer", () -> new NFUJsonParser<>(Integer.class, JsonElement::getAsInt, 0));
    public static final NFURegistry.Accessor<NFUJsonParser<Double>> DOUBLE =
        REG.register("double", () -> new NFUJsonParser<>(Double.class, JsonElement::getAsDouble, 0d));
    public static final NFURegistry.Accessor<NFUJsonParser<String>> STRING =
        REG.register("string", () -> new NFUJsonParser<>(String.class, JsonElement::getAsString, ""));

    public NFUJsonParser(Class<T> objClass, Function<JsonElement, T> parser, T fallback) {
        this.objClass = objClass;
        this.parser = parser;
        this.fallback = fallback;
    }

    public T parse(JsonElement data) {
        try {
            return parser.apply(data);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

}
