package net.sodiumzh.nfu.object;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * An object wrapper that is not loaded on initialization. It can be loaded only on manually calling load(), before which
 * the object is not constructed or available.
 */
public class DeferredLoaded<T> {

    private final Supplier<T> loader;
    private @Nullable T object;
    private volatile boolean loaded;

    public DeferredLoaded(Supplier<T> loader) {
        this.loader = loader;
    }

    public T get() {
        if (!loaded)
            throw new IllegalStateException("NFU DeferredLoaded: access before loading.");
        return object;
    }

    public synchronized void load() {
        if (!this.loaded) {
            object = loader.get();
            loaded = true;
        }
    }

    public T loadAndGet() {
        if (!loaded) this.load();
        return this.get();
    }
}
