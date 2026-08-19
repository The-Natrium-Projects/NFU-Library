package net.sodiumzh.nfu.util;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Utilities for vanilla or NeoForge registration-related things
 */
public class NFURegistrationStatics {

    public static <T> Optional<Supplier<? extends T>> getDeferredRegisterSupplier(DeferredRegister<T> register, String key) {
        Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries = NFUReflectionStatics.forceGet(
            register, DeferredRegister.class, "entries").cast();
        if (entries == null) return Optional.empty();
        DeferredHolder<T, ? extends T> entryKey = null;
        for (DeferredHolder<T, ? extends T> entry: register.getEntries()) {
            if (entry.getId().getPath().equals(key)) {
                entryKey = entry;
                break;
            }
        }
        if (entryKey == null) return Optional.empty();
        return Optional.ofNullable(entries.get(entryKey));
    }

}
