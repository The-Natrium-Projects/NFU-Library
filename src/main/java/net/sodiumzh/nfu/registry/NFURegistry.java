package net.sodiumzh.nfu.registry;

import com.google.common.collect.HashBiMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.sodiumzh.nfu.annotation.NotYetImplemented;
import net.sodiumzh.nfu.exception.DuplicateRegistryEntryException;
import net.sodiumzh.nfu.network.AvailableSide;
import net.sodiumzh.nfu.network.NFUDataSerializer;
import net.sodiumzh.nfu.object.DirectedGraphNode;
import net.sodiumzh.nfu.object.LimitedMutable;
import net.sodiumzh.nfu.util.NFUDebugStatics;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
/**
 * A simple lazy-loaded registry. Built after Forge registry and mod loading.
 * Note that this is NOT linked to Minecraft registry system.
 */
public class NFURegistry<T> implements DirectedGraphNode<NFURegistry<?>>
{
    public static final LimitedMutable<Boolean> COMMON_SETUP_DONE = new LimitedMutable<>(false, 1);
    public static final LimitedMutable<Boolean> CLIENT_SETUP_DONE = new LimitedMutable<>(false, 1);
    public static final LimitedMutable<Boolean> SERVER_SETUP_DONE = new LimitedMutable<>(false, 1);

    /** Collection of all declared registries. */
    private static final HashBiMap<ResourceLocation, NFURegistry<?>> REGISTRIES = HashBiMap.create();
    /** Internal map. Access must be synchronized on {@code this}. */
    private final HashMap<ResourceLocation, Entry<? extends T>> table = new HashMap<>();
    /** Indicates when the registry should be built. */
    private LoadTiming loadTiming = LoadTiming.COMMON_SETUP;
    /** Indicates which side this registry can be accessed. Values are all {@code null} if on the wrong side. */
    private AvailableSide availableSide = AvailableSide.BOTH;
    /** Reverse map for key getting. A {@code null} reference indicates the registry hasn't been built.
     * Volatile for safe publication: the fully-built map is published atomically on loading and never
     * mutated afterwards (late registration replaces it with a copied map). */
    private volatile @Nullable HashMap<T, ResourceLocation> reverseMap = null;
    /** Indicates this registry should be loaded before the listed registries. */
    private final List<NFURegistry<?>> shouldLoadBefore = new ArrayList<>();
    /** If false, access will never be allowed before loading and will always return null. */
    private boolean allowsAccessBeforeLoading = true;
    /**
     * If true, the values will keep {@code null} if entry construction throws an exception on registry loading. Otherwise, the exception will be thrown out.
     * <p>This configuration doesn't throw if an entry loads correctly but gets a {@code null}, and doesn't guarantee non-null if false.
     * <p>This configuration doesn't impact access attempts before register loading. In this case, it will still return null if exception happens.
     */
    private boolean allowsLoadingFailures = false;

    // Methods below //

    /**
     * @param registryKey Key of this registry in the table of all registries.
     */
    public NFURegistry(ResourceLocation registryKey)
    {
        REGISTRIES.put(registryKey, this);
    }


    // Registry of registries related //

    /**  Collection of all registries */
    public static Map<ResourceLocation, NFURegistry<?>> allRegistries()
    {
        return REGISTRIES;
    }


    public static NFURegistry<?> registryByKey(ResourceLocation key)
    {
        return REGISTRIES.get(key);
    }

    /**
     * Get this registry's key in the registry of all {@code NFURegsitry} instances.
     */
    public ResourceLocation getKeyOfRegistry()
    {
        return REGISTRIES.inverse().get(this);
    }

    // Accessibility policies //

    /**
     * Check if the registry is called on the correct logical side.
     */
    public boolean isCorrectSide() {
        return this.availableSide.equals(AvailableSide.BOTH) ||
            (EffectiveSide.get().isClient() && this.availableSide.equals(AvailableSide.CLIENT)) ||
            (EffectiveSide.get().isServer() && this.availableSide.equals(AvailableSide.SERVER));
    }

    public boolean isAvailableOnClient() {
        return this.availableSide.equals(AvailableSide.BOTH) || this.availableSide.equals(AvailableSide.CLIENT);
    }

    public boolean isAvailableOnServer() {
        return this.availableSide.equals(AvailableSide.BOTH) || this.availableSide.equals(AvailableSide.SERVER);
    }

    /**
     * Set this registry should be only available on a given logical side. If {@link Accessor#get()} is called on the
     * wrong side, it will always return {@code null}.
     */
    public NFURegistry<T> setSide(AvailableSide side) {
        this.availableSide = side;
        return this;
    }

    public boolean allowsAccessBeforeLoading() {
        return allowsAccessBeforeLoading;
    }

    public NFURegistry<T> setAllowsAccessBeforeLoading(boolean value) {
        this.allowsAccessBeforeLoading = value;
        return this;
    }

    // Loading related //

    /**
     * Load all values that haven't been loaded, and label the registry as loaded (by publishing the reverse map).
     * It doesn't impact values already loaded.
     * <p>Synchronized: loading must not race with concurrent loading or late registration. The reverse map is
     * built into a local map and published atomically at the end, so readers never observe a half-built map.
     */
    public synchronized void load()
    {
        // For those available on both sides and loaded on side setup, it may be loaded twice, and the first wins
        if (this.isLoaded()) return;
        HashMap<T, ResourceLocation> newReverseMap = new HashMap<>();
        this.table.forEach((k, v) -> {
            if (!v.loaded) {
                v.loadFinal();
                T value = v.get();
                if (value != null) {
                    if (newReverseMap.containsKey(value))
                        throw DuplicateRegistryEntryException.duplicateValue(newReverseMap.get(value).toString(), k.toString());
                    newReverseMap.put(value, k);
                }
            }
        });
        // Publish the fully-built map atomically. This volatile write happens-before any read that observes it.
        this.reverseMap = newReverseMap;
    }

    public boolean isLoaded() {
        return this.reverseMap != null;
    }

    public LoadTiming getLoadTiming()
    {
        return this.loadTiming;
    }

    public NFURegistry<T> setLoadTiming(LoadTiming phase) {
        this.loadTiming = phase;
        return this;
    }

    /**
     * If true, the values will keep {@code null} if entry construction throws an exception on registry loading. Otherwise, the exception will be thrown out.
     * <p>This configuration doesn't throw if an entry loads correctly but gets a {@code null}, and doesn't guarantee non-null if false.
     * <p>This configuration doesn't impact access attempts before register loading. In this case, it will still return null if exception happens.
     */
    public boolean allowsLoadingFailures() {
        return this.allowsLoadingFailures;
    }

    /**
     * If set true, the values will keep {@code null} if an entry construction throws an exception on registry loading. Otherwise, the exception will be thrown out.
     * <p>This configuration doesn't throw if an entry loads correctly but gets a {@code null}, and doesn't guarantee non-null if false.
     * <p>This configuration doesn't impact access attempts before register loading. In this case, it will still return null if exception happens.
     */
    public NFURegistry<T> setAllowsLoadingFailures(boolean value) {
        this.allowsLoadingFailures = value;
        return this;
    }

    /**
     * Set that this registry should be loaded before the following registries.
     */
    public NFURegistry<T> setLoadBefore(NFURegistry<?>... registries) {
        for (NFURegistry<?> reg: registries) {
            this.shouldLoadBefore.add(reg);
            // Detect cycle
            List<NFURegistry<?>> cycle = this.getCycle();
            if (cycle != null) {
                this.shouldLoadBefore.remove(reg);
                StringBuilder cycleInfo = new StringBuilder(cycle.get(0).getKeyOfRegistry().toString());
                for (int i = 1; i < cycle.size(); ++i)
                    cycleInfo.append(" -> ").append(cycle.get(i).getKeyOfRegistry().toString());
                throw new IllegalArgumentException("NaUtilsRegistry loading order error: cyclic dependency detected.\n" +
                    "Cycle: " + cycleInfo);
            }
        }
        return this;
    }

    /**
     * Set that this registry should be loaded after the following registries.
     */
    public NFURegistry<T> setLoadAfter(NFURegistry<?>... registries) {
        for (NFURegistry<?> reg: registries) {
            reg.setLoadBefore(this);
        }
        return this;
    }

    public boolean shouldLoadBefore(NFURegistry<?> other) {
        return this.shouldLoadBefore.contains(other) && !other.shouldLoadBefore.contains(this);
    }

    public boolean shouldLoadAfter(NFURegistry<?> other) {
        return !this.shouldLoadBefore.contains(other) && other.shouldLoadBefore.contains(this);
    }

    /**
     * @return Registries that should be loaded after this registry. This method is only called on deciding
     * registry loading ordering.
     */
    @ApiStatus.Internal
    @Override
    public Set<NFURegistry<?>> children() {
        return Set.copyOf(this.shouldLoadBefore);
    }

    // Queries //

    /**
     * Total amount of entries in this registry, including unloaded and error entries.
     */
    public int size() {
        synchronized (this) {
            return table.size();
        }
    }

    public boolean isEmpty() {
        synchronized (this) {
            return table.isEmpty();
        }
    }

    /**
     * Whether the registry contains the given key. It doesn't check if the value is loaded or null.
     */
    public boolean containsKey(ResourceLocation key) {
        synchronized (this) {
            return table.containsKey(key);
        }
    }

    /**
     * Whether the registry contains a given value. ONLY available after registry building.
     */
    public boolean containsValue(T value)
    {
        if (!this.isLoaded()) {
            if (this.getLoadTiming().equals(LoadTiming.FIRST_ACCESS)) {
                this.load();
            }
            else return false;
        }
        // Non-null here: load() either publishes the map or throws.
        return this.reverseMap.containsKey(value);
    }

    /**
     * Get the value from key. Note that if the supplier throws an exception,
     * it will not crash but print stacktrace and return null.
     */
    @Nullable
    public T getValue(ResourceLocation key) {
        Entry<? extends T> entry;
        synchronized (this) {
            entry = table.get(key);
        }
        if (entry == null) return null;
        return entry.get();
    }

    public Optional<T> getOptionalValue(ResourceLocation key) {
        return Optional.ofNullable(getValue(key));
    }

    @Nullable
    public ResourceLocation getKey(T value) {
        if (!this.isLoaded()) {
            if (this.getLoadTiming().equals(LoadTiming.FIRST_ACCESS)) {
                this.load();
            }
            else return null;
        }
        return this.reverseMap.get(value);
    }

    public Optional<ResourceLocation> getOptionalKey(T value) {
        return Optional.ofNullable(this.getKey(value));
    }

    public Set<ResourceLocation> keySet() {
        // Snapshot: the table may be concurrently mutated by registration.
        synchronized (this) {
            return Set.copyOf(table.keySet());
        }
    }

    /**
     * Get all values.
     * <p>If a value appears twice in the registry, it will appear twice in this list. Null values will be removed.
     * <p>Note: this method will cause all entries to generate values. Take care of the timing if the values are valid!
     */
    public Set<? extends T> values() {
        if (!this.isLoaded()) {
            if (this.getLoadTiming().equals(LoadTiming.FIRST_ACCESS)) {
                this.load();
            }
            else return Set.of();
        }
        return this.reverseMap.keySet();
    }

    /**
     * Register an object from supplier.
     * @return An {@code Accessor} for getting the object, so that you can assign it to a
     * static field. Its usage is similar to {@link RegistryObject}.
     * <p>It's recommended to use {@link NFURegistryEntryCollection} instead (just like using {@link DeferredRegister}).
     * Directly registering may cause issues if the class in which you're registering objects is not loaded on setup phase.
     */
    public synchronized <U extends T> Accessor<U> register(ResourceLocation key, Supplier<U> supplier)
    {
        if (this.containsKey(key)) throw DuplicateRegistryEntryException.registeredTwice(key.toString());
        Entry<U> entry = new Entry<>(this, supplier, key);
        this.table.put(key, entry);
        // Handle case when the entry is registered after loading (not recommended)
        if (this.isLoaded()) {
            entry.loadFinal();
            T value = entry.get();
            if (value != null) {
                if (this.reverseMap.containsKey(value))
                    throw DuplicateRegistryEntryException.duplicateValue(this.reverseMap.get(value).toString(), key.toString());
                // Never mutate the published map; atomically replace it with an updated copy.
                HashMap<T, ResourceLocation> updated = new HashMap<>(this.reverseMap);
                updated.put(value, key);
                this.reverseMap = updated;
            }
        }
        return new Accessor<>(entry);
    }

    /**
     * Register an object from supplier if it's not present.
     * @return An {@code Optional<Accessor>} if registered. {@code Optional#empty()} if the entry exists.
     */
    public synchronized <U extends T> Optional<Accessor<U>> registerIfAbsent(ResourceLocation key, Supplier<U> supplier)
    {
        if (this.containsKey(key)) return Optional.empty();
        return Optional.of(this.register(key, supplier));
    }

    /**
     * Only for {@link NFURegistryEntryCollection}.
     */
    @ApiStatus.Internal
    synchronized void registerRaw(ResourceLocation key, Entry<? extends T> value)
    {
        this.table.put(key, value);
    }

    static class Entry<T>
    {
        private final Supplier<T> supplier;
        /** Volatile: safely published together with {@link #loaded}. */
        private volatile @Nullable T cachedValue;
        private final NFURegistry<? super T> registry;
        private final ResourceLocation key;
        /** Volatile: the publication flag. A read of {@code true} happens-after the write of {@link #cachedValue}. */
        private volatile boolean loaded = false;

        public Entry(@Nonnull NFURegistry<? super T> registry, @Nonnull Supplier<T> supplier, ResourceLocation key)
        {
            this.supplier = supplier;
            this.key = key;
            this.cachedValue = null;
            this.registry = registry;
        }

        /**
         * Get value from the supplier. Note that once the supplier output a valid value,
         * it won't rerun (i.e. the value won't change) until {@code regenerate} is called.
         */
        @Nullable
        public T get()
        {
            if (!registry.isCorrectSide())
                return null;
            if (!this.isLoaded()) {
                /* this.loadFinal() will be invoked on registry loading. If not accessible before load,
                 any access before this.loaded is set will be illegal, and this.loaded will be set true only
                 loadFinal(), and tryLoad() will never be invoked */
                if (this.registry.getLoadTiming().equals(LoadTiming.FIRST_ACCESS)) {
                    // registry load() will call Entry.get(), so load self first to prevent inf recursion
                    this.loadFinal();
                    this.registry.load();
                    return this.cachedValue;
                }
                if (!registry.allowsAccessBeforeLoading())
                    return null;
                if (this.cachedValue != null)
                    throw new IllegalStateException("NFURegistry.Entry value is present but not labeled loaded.");
                // Double-checked locking: keep the post-load read path lock-free while ensuring
                // only one thread runs the supplier during concurrent pre-load access.
                synchronized (this) {
                    if (!this.isLoaded())
                        this.tryLoad();
                }
                return cachedValue;
            }
            else return cachedValue;
        }

        /**
         * Not synchronized itself: must only be called from within {@code synchronized (this)} in {@link #get()}.
         */
        private void tryLoad() {
            try {
                cachedValue = supplier.get();
                if (cachedValue != null) this.loaded = true;
            } catch (RuntimeException e)
            {
                // If running supplier encountered error, don't crash but
                // set the cache to null so that the supplier will rerun next time.
                NFUDebugStatics.errorOnce(NFURegistry.Accessor.class,
                    "Registry entry getting value failed. Registry=\"" + this.registry.getKeyOfRegistry()
                        + "\", key=\"" + this.key + "\".");
                cachedValue = null;
            }
        }

        /**
         * Load the value, and label this entry as built. A built entry will never try loading again even if it's null.
         * <p>Synchronized so that the value write and the {@code loaded} flag are published atomically
         * w.r.t. concurrent {@link #get()} calls.
         */
        public synchronized void loadFinal() {
            if (!this.isLoaded()) {
                try {
                    this.cachedValue = this.supplier.get();
                } catch (RuntimeException e) {
                    if (this.registry.allowsLoadingFailures) {
                        this.cachedValue = null;
                    }
                    else throw e;
                }
            }
            this.loaded = true;
        }

        public boolean isLoaded() {
            return this.loaded;
        }
    }

    public static class Accessor<T> implements Supplier<T>
    {
        private final Entry<T> entry;
        private volatile boolean validated;  // Labels whether this entry has been registered into a registry. If it's false, the get() will always return null.

        Accessor(Entry<T> entry) {
            this.entry = entry;
            this.validated = true;
        }

        static <U> Accessor<U> createInvalid(Entry<U> entry)
        {
            Accessor<U> res = new Accessor<>(entry);
            res.validated = false;
            return res;
        }

        @Override
        public T get()
        {
            if (!validated) return null;
            return entry.get();
        }

        Accessor<T> validate() {this.validated = true; return this;}
    }

    public static enum LoadTiming {
        /**
         * Load entries on common setup.
         */
        COMMON_SETUP,
        /**
         * Load entries on client and/or server setup.
         * <p>In single-player game, client setup will happen first and load the values, which is shared between server and client.
         */
        SIDE_SETUP,
        /**
         * Do not load on a specific phase, but load all values on the first access in any form.
         * <p>Note: this option is relatively risky. Ensure that all entries are possible to be loaded before access.
         */
        FIRST_ACCESS;
    }

    public static List<NFURegistry<?>> sortByLoadingOrder(Collection<NFURegistry<?>> raw) {
        return DirectedGraphNode.topologicalSort(raw);
    }

    /**
     * NYI
     * Indicates how it should handle sync. This record should exist on both sides no matter on which side
     * the registry is available.
     * @param shouldSync If true, it will sync the data to another side.
     * @param from Where the data is posted from.
     * @param encoder How the data should be written to buffer.
     * @param decoder How the data should be generated from buffer.
     */
    @NotYetImplemented
    public static record SyncPolicy<T>(boolean shouldSync, LogicalSide from, BiConsumer<FriendlyByteBuf, T> encoder, Function<FriendlyByteBuf, T> decoder) {

        public static <T> SyncPolicy<T> noSync() {
            return new SyncPolicy<>(false, null, null, null);
        }

        public static <T> SyncPolicy<T> fromServer(@Nonnull BiConsumer<FriendlyByteBuf, T> encoder,
                                                   @Nonnull Function<FriendlyByteBuf, T> decoder) {
            return new SyncPolicy<>(true, LogicalSide.SERVER, encoder, decoder);
        }

        public static <T> SyncPolicy<T> fromServer(NFUDataSerializer<T> serializer) {
            return fromServer(serializer::write, serializer::read);
        }

        public static <T> SyncPolicy<T> fromClient(@Nonnull BiConsumer<FriendlyByteBuf, T> encoder,
                                                   @Nonnull Function<FriendlyByteBuf, T> decoder) {
            return new SyncPolicy<>(true, LogicalSide.CLIENT, encoder, decoder);
        }

        public static <T> SyncPolicy<T> fromClient(NFUDataSerializer<T> serializer) {
            return fromClient(serializer::write, serializer::read);
        }

    }

}
