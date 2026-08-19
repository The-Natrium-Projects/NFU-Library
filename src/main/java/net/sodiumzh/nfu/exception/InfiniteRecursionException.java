package net.sodiumzh.nfu.exception;

/**
 * Indicates a detected infinite recursion.
 */
public class InfiniteRecursionException extends RuntimeException {
    public InfiniteRecursionException(String message) {
        super(message);
    }
}
