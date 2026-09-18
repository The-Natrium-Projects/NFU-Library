package net.sodiumzh.nfu.object;

import net.minecraftforge.fml.util.thread.EffectiveSide;

import javax.annotation.Nullable;

public class SideLocal<T> {

    private volatile T serverValue;
    private volatile T clientValue;
    private volatile boolean serverInitialized = false;
    private volatile boolean clientInitialized = false;
    private final Object serverLock = new Object();
    private final Object clientLock = new Object();
    private final Supplier<T> initializer;

    public SideLocal(Supplier<T> initializer) {
        this.initializer = initializer;
    }

    public SideLocal() {
        this(() -> null);
    }

    public T get() {
        if (EffectiveSide.get().isClient()) {
            if (!clientInitialized) {
                synchronized (clientLock) {
                    if (!clientInitialized) {
                        clientValue = initializer.get();
                        clientInitialized = true; // set flag AFTER value
                    }
                }
            }
            return clientValue;
        } else {
            if (!serverInitialized) {
                synchronized (serverLock) {
                    if (!serverInitialized) {
                        serverValue = initializer.get();
                        serverInitialized = true;
                    }
                }
            }
            return serverValue;
        }
    }

    public SideLocal<T> set(@Nullable T value) {
        if (EffectiveSide.get().isClient()) {
            synchronized (clientLock) {
                clientValue = value;
                clientInitialized = true;
            }
        } else {
            synchronized (serverLock) {
                serverValue = value;
                serverInitialized = true;
            }
        }
        return this;
    }
}
