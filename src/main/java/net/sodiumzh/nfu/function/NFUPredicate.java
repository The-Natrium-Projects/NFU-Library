package net.sodiumzh.nfu.function;

import java.util.function.Function;
import java.util.function.Predicate;

public class NFUPredicate<T> extends NFUOperator<T, Boolean> implements Predicate<T> {

    public NFUPredicate(Class<T> paramClass, Predicate<T> predicate) {
        super(paramClass, Boolean.class, predicate::test);
    }

    @Override
    public boolean test(T t) {
        return this.apply(t);
    }
}
