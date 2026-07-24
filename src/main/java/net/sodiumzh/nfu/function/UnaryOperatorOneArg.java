package net.sodiumzh.nfu.function;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A unary operator with one argument.
 * <p>Unlike {@code BiFunction}, {@code thenApply} and {@code thenAccept} of this class accepts the same value as argument of the next action.
 */
@FunctionalInterface
public interface UnaryOperatorOneArg<T, ARG> {

    public T apply(T in, ARG arg);

    @ApiStatus.NonExtendable
    public default UnaryOperatorOneArg<T, ARG> thenApply(UnaryOperatorOneArg<T, ARG> next) {
        return (in, arg) -> {
            T t1 = this.apply(in, arg);
            return next.apply(in, arg);
        };
    }

    public default UnaryOperatorOneArg<T, ARG> thenApply(UnaryOperator<T> next) {
        return this.thenApply((t, arg) -> next.apply(t));
    }

    public default UnaryOperatorOneArg<T, ARG> thenAccept(BiConsumer<T, ARG> action) {
        return this.thenApply((t, arg) -> {action.accept(t, arg);return t;});
    }

    public default UnaryOperatorOneArg<T, ARG> thenAccept(Consumer<T> action) {
        return this.thenApply((t, arg) -> {action.accept(t);return t;});
    }
}
