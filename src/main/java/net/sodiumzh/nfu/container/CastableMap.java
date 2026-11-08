package net.sodiumzh.nfu.container;

import net.sodiumzh.nfu.object.CastableObject;

import java.util.HashMap;
import java.util.Optional;

/**
 * A {@link HashMap} utility to easily cast the values to any classes.
 */
public class CastableMap<K, V> extends HashMap<K, V> {

    public <T> T getAs(K key, Class<T> asClass) {
        V res = this.get(key);
        if (res == null) return null;
        return asClass.isAssignableFrom(res.getClass()) ? (T)res : null;
    }

    public Optional<V> getOptional(K key) {
        return Optional.ofNullable(this.get(key));
    }

    public <T> Optional<T> getOptionalAs(K key, Class<T> asClass) {
        return Optional.ofNullable(getAs(key, asClass));
    }

    public CastableObject getCastable(K key) {
        return new CastableObject(this.get(key));
    }
}
