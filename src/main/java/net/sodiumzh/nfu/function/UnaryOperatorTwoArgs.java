package net.sodiumzh.nfu.function;

import org.apache.commons.lang3.function.TriFunction;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A unary operator with two arguments.
 * <p>Unlike {@code TriFunction}, {@code thenApply} and {@code thenAccept} of this class accepts the same value as argument of the next action.
 */
@FunctionalInterface
public interface UnaryOperatorTwoArgs<T, ARG1, ARG2> {

    public T apply(T in, ARG1 arg1, ARG2 arg2);

    public default UnaryOperatorTwoArgs<T, ARG1, ARG2> thenApply(UnaryOperatorTwoArgs<T, ARG1, ARG2> next) {
        return (t, a1, a2) -> {
            T t1 = this.apply(t, a1, a2);
            return next.apply(t1, a1, a2);
        };
    }

    public default UnaryOperatorTwoArgs<T, ARG1, ARG2> thenApply(UnaryOperator<T> next) {
        return this.thenApply((t, arg1, arg2) -> next.apply(t));
    }

    public default UnaryOperatorTwoArgs<T, ARG1, ARG2> thenAccept(TriConsumer<T, ARG1, ARG2> action) {
        return this.thenApply((t, arg1, arg2) -> {action.accept(t, arg1, arg2);return t;});
    }

    public default UnaryOperatorTwoArgs<T, ARG1, ARG2> thenAccept(Consumer<T> action) {
        return this.thenApply((t, arg1, arg2) -> {action.accept(t);return t;});
    }
}
