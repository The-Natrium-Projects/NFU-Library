package net.sodiumzh.nfu.container;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Base interface of containers which have the structure of stack.
 */
public interface IStackContainer<T> {

    /**
     * Push an element to the stack top.
     */
    public void push(@Nonnull T elem);

    /**
     * Pop and return the top element. If the stack is already empty, do nothing and return null.
     */
    @Nullable
    public T pop();

    /**
     * Get the top element.
     */
    public Optional<T> getTop();

    /**
     * Get a list from the top to the bottom.
     */
    public List<T> backTraceList();

    /**
     * Whether this stack is empty i.e. doesn't contain any element.
     */
    public boolean isEmpty();

    /**
     * Remove all elements of the stack.
     */
    public void clear();

    /**
     * Pop the target element and all elements above it. Return if the popping action is performed.
     * If the target is not present in the stack, do nothing and return false.
     */
    public default boolean pop(T target) {
        List<T> backtrace = backTraceList();
        if (backtrace.contains(target)) {
            while (true) {
                assert (!this.isEmpty());
                T popped = this.pop();
                if (Objects.equals(target, popped))
                    return true;
            }
        }
        else return false;
    }

}
