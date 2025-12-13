package net.sodiumzh.nfu.container;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class StackContainer<T> implements IStackContainer<T> {

    private final List<T> list = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static <T> StackContainer<T> copyOf(IStackContainer<T> other) {
        StackContainer<T> res = new StackContainer<>();
        res.list.addAll(other.backTraceList().stream().sorted((Comparator<? super T>) Comparator.reverseOrder()).toList());
        return res;
    }

    @Override
    public void push(@Nonnull T elem) {
        list.add(elem);
    }

    @Override
    public T pop() {
        if (list.isEmpty())
            return null;
        else return list.remove(list.size() - 1);
    }

    @Override
    public Optional<T> getTop() {
        if (this.isEmpty()) return Optional.empty();
        return Optional.ofNullable(list.get(list.size() - 1));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> backTraceList() {
        return list.stream().sorted((Comparator<? super T>) Comparator.reverseOrder()).toList();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public void clear() {
        list.clear();
    }
}
