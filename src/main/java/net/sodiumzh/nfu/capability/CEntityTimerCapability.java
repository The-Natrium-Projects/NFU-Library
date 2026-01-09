package net.sodiumzh.nfu.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.eventbus.api.Event;
import net.sodiumzh.nfu.annotation.DontCallManually;
import net.sodiumzh.nfu.annotation.DontOverride;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * {@code CEntityTimerCapability} is a capability template that has a timer map which updates every tick.
 * </p>
 * <p>
 * This is a ticking capability: you need to register it using {@link CEntityTickingCapability#registerTicking}.
 * The {@code tickTimer} method will be auto-called on tick together with {@code tick}; you do not need to call it manually.
 * </p>
 * <p>
 * Capabilities using this interface must have a field containing the timer map (a {@code Map<String, Integer>})
 * and return it via {@code getTimerMap()}.
 * </p>
 * <p>
 * Each timer entry decrements every tick. If a timer reaches zero, it is removed from the map and an {@link ExpireEvent}
 * is posted to {@code MinecraftForge.EVENT_BUS}.
 * Negative values mean a timer will never expire until manually removed.
 * </p>
 * <p>
 * Use {@link #saveTimer()} and {@link #loadTimerFromNBT(CompoundTag)} to persist and restore the timer map.
 * </p>
 */
@AutoRegisterCapability
public interface CEntityTimerCapability<T extends Entity> extends CEntityTickingCapability<T> {

    /**
     * Get the timer map for this capability. This must be implemented by the subclass.
     * Keys are timer names, values are tick counts.
     * @return the timer map
     */
    @DontCallManually
    public Map<String, Integer> getTimerMap();

    /**
     * Get the remaining tick time for the given timer key, or 0 if absent.
     */
    @DontOverride
    public default int getTimerRemainingTime(String key) {
        return getTimerMap().getOrDefault(key, 0);
    }

    /**
     * Check if a timer with the given key exists and has non-zero remaining time.
     */
    @DontOverride
    public default boolean hasTimer(String key) {
        return getTimerRemainingTime(key) != 0;
    }

    /**
     * Set the given timer. If the timer exists, its remaining ticks will be overwritten.
     * Negative values mean the timer never expires unless manually removed.
     * <p>Note: If {@code ticks} is 0, the timer ends immediately without posting an event.
     * This operation is not recommended; use {@code removeTimer} instead if you want an expiration event.
     */
    @DontOverride
    public default void setTimer(String key, int ticks) {
        if (ticks != 0)
            getTimerMap().put(key, ticks);
        else removeTimer(key, false);
    }

    /**
     * Add an entry to the timer map, but will not overwrite the value if the key exists
     * and the remaining time is longer than the input.
     * Negative values create a permanent entry.
     */
    @DontOverride
    public default void safeSetTimer(String key, int ticks) {
        if (!(getTimerRemainingTime(key) > 0 && ticks > 0 && ticks < getTimerRemainingTime(key)))
            setTimer(key, ticks);
    }

    /**
     * Remove the timer with the given key.
     * If {@code postEvent} is true, an {@link ExpireEvent} will be posted to the Forge event bus.
     */
    @DontOverride
    public default void removeTimer(String key, boolean postEvent) {
        this.getTimerMap().remove(key);
        if (postEvent) MinecraftForge.EVENT_BUS.post(new ExpireEvent(this, key));
    }

    /**
     * <p>
     * Decrements all timers each tick, removes timers whose value reaches zero,
     * and posts an {@link ExpireEvent} to {@code MinecraftForge.EVENT_BUS} for each timer that is removed.
     * </p>
     * <p>
     * This method is automatically called on each tick as part of the ticking capability system. Do not call it manually.
     * </p>
     */
    @DontOverride
    public default void tickTimer()
    {
        var map = getTimerMap();
        Set<String> removal = new HashSet<>();
        for (String key: map.keySet()) {
            int oldVal = map.get(key);
            if (oldVal > 0)
                map.put(key, oldVal - 1);
            else if (oldVal == 0) {
                removal.add(key);
            }
        }
        for (String key: removal) {
            map.remove(key);
        }
        for (String key: removal) {
            MinecraftForge.EVENT_BUS.post(new ExpireEvent(this, key));
        }
    }

    /**
     * Save the timer map to an NBT tag.
     * Each entry will be stored as an int under its key.
     * @return a {@link CompoundTag} containing this timer state
     */
    @DontOverride
    public default CompoundTag saveTimer() {
        CompoundTag nbt = new CompoundTag();
        var map = getTimerMap();
        for (String key: map.keySet()) {
            nbt.put(key, IntTag.valueOf(map.get(key)));
        }
        return nbt;
    }

    /**
     * Load the timer map from the given NBT data.
     * This will clear the current timer map before loading from NBT.
     * Only use NBT produced by {@link #saveTimer()}.
     * @param nbt the tag to read from
     */
    @DontOverride
    public default void loadTimerFromNBT(CompoundTag nbt) {
        var map = getTimerMap();
        map.clear();
        for (String key: nbt.getAllKeys()) {
            map.put(key, nbt.getInt(key));
        }
    }

    /**
     * <p>
     * Fired when a timer expires and is removed from an entity capability.
     * </p>
     * <p>
     * This event is posted to the Forge event bus when a timer managed by {@link CEntityTimerCapability}
     * expires and is removed (by {@link #tickTimer()} or {@link #removeTimer(String, boolean)} with {@code postEvent} true).
     * Listeners can use this to observe or react to the ending of specific entity-local timers (for example, for triggering effects or state changes).
     * </p>
     */
    public static class ExpireEvent extends Event {
        private final CEntityTimerCapability<?> cap;
        private final String key;

        /**
         * Create a new ExpireEvent.
         * @param cap the timer capability whose timer expired
         * @param key the timer key that expired
         */
        public ExpireEvent(CEntityTimerCapability<?> cap, String key) {
            this.cap = cap;
            this.key = key;
        }

        /**
         * Returns the capability whose timer expired.
         */
        public CEntityTimerCapability<?> getCapability() {
            return cap;
        }

        /**
         * Returns the key of the timer that expired.
         */
        public String getKey() {
            return key;
        }
    }
}