package net.sodiumzh.nfu.entity.component.preset;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.container.ITable2D;
import net.sodiumzh.nfu.container.Table2D;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.container.Tuple3;
import net.sodiumzh.nfu.entity.component.EntityComponentAPI;
import net.sodiumzh.nfu.entity.component.EntityComponentBase;
import net.sodiumzh.nfu.event.NFUEntityEvent;
import org.checkerframework.checker.signature.qual.CanonicalNameOrEmpty;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * A utility component as a timer.
 * <p><b>Main features:</b>
 * <p><b>(a)Timers</b>
 * <ul>
 * Timers count down and notify when time-up. Timers are divided to "general timers" (using a string as key only) and "UUID-specific timers"
 * (using a UUID and a string as keys). Operations of these two types of timers are separated. It also supports looped timers (auto-restarting for given times after timer-up).
 * Timers can be serialized.
 * <p>There are three ways to listen to timer-up notification:
 * <li>Extend {@code onGeneralTimerExpire} or {@code onUUIDSpecificTimerExpire};
 * <li>Let the corresponding entity implement {@link IEntityTimerComponentAccess} and override {@link IEntityTimerComponentAccess#onTimerExpire};
 * <li>Listen to {@link EntityTimerComponent.ExpireEvent}.
 * </ul>
 * <p><b>(b)Delayed actions</b>
 * <ul>
 * Delayed actions invoke a given action after a given time. Unlike timers, the action is specified as a functional argument
 * on adding, but not pre-defined by method override or event listener. It also supports loop, but can NOT serialize.
 * <p> Delayed actions don't have listeners, but just auto invoke the actions after expiration.
 * </ul>
 */
public class EntityTimerComponent<T extends Entity> extends EntityComponentBase<T> {

    private final Map<String, Timer> generalTimers = new HashMap<>();
    private final ITable2D<UUID, String, Timer> uuidSpecificTimers = new Table2D<>();
    private final Map<UUID, Tuple2<Runnable, Timer>> delayedActionTimers = new HashMap<>();  // Key is the access id, provided on adding timer
    //private final Map<UUID, Map<UUID, Tuple2<Consumer<UUID>, Timer>>> uuidSpecificDelayedActionTimers = new HashMap<>(); // The entry map key uuid is an internal identifier, not present in API

    private final List<String> expiredKeys = new ArrayList<>();  // Temporary list, only works on tick
    private final List<ITable2D.KeyPair<UUID, String>> expiredIdSpecificKeys = new ArrayList<>();
    private final List<UUID> expiredActions = new ArrayList<>();   // Temporary list, only works on tick
   // private final List<Tuple2<UUID, UUID>> expiredIdSpecificActions = new ArrayList<>();    // First UUID is the key UUID; second is internal identifier

    public EntityTimerComponent(T entity) {
        super(entity);
    }

    public final boolean isDefaultTimerComponent() {
        return this.equals(EntityComponentAPI.getDefaultTimer(this.getEntity()));
    }

    public void tick() {
        for (var entry: generalTimers.entrySet()) {
            entry.getValue().tick();
            if (entry.getValue().isTerminated() || entry.getValue().isJustFinishedALoop()) {
                // Notify this
                this.onGeneralTimerExpire(entry.getKey(), entry.getValue().isTerminated());
                // Notify entity class if using the access interface
                if (this.getEntity() instanceof IEntityTimerComponentAccess user) {
                    user.onTimerExpire(this, entry.getKey(), entry.getValue().isTerminated(), null);
                    if (this.isDefaultTimerComponent())
                        user.onDefaultTimerExpire(entry.getKey(), entry.getValue().isTerminated(), null);
                }
                // Notify event listeners
                MinecraftForge.EVENT_BUS.post(new ExpireEvent(this.getEntity(), this, entry.getKey(), entry.getValue().isTerminated(), null));
                if (entry.getValue().isTerminated()) expiredKeys.add(entry.getKey());
            }
        }
        this.uuidSpecificTimers.entryStream().forEach(entry -> {
            entry.value().tick();
            if (entry.value().isTerminated() || entry.value().isJustFinishedALoop()) {
                // TODO Notify this
                // Notify entity class if using the access interface
                if (this.getEntity() instanceof IEntityTimerComponentAccess user) {
                    user.onTimerExpire(this, entry.columnKey(), entry.value().isTerminated(), entry.rowKey());
                    if (this.isDefaultTimerComponent())
                        user.onDefaultTimerExpire(entry.columnKey(), entry.value().isTerminated(), entry.rowKey());
                }// Notify event listeners
                MinecraftForge.EVENT_BUS.post(new ExpireEvent(this.getEntity(), this, entry.columnKey(), entry.value().isTerminated(), entry.rowKey()));
                if (entry.value().isTerminated()) expiredIdSpecificKeys.add(new ITable2D.KeyPair<>(entry.rowKey(), entry.columnKey()));
            }
        });
        for (var entry: delayedActionTimers.entrySet()) {
            // Key: internal UUID; Value: Tuple2{action, timer}
            entry.getValue().getB().tick();
            if (entry.getValue().getB().isTerminated() || entry.getValue().getB().isJustFinishedALoop()) {
                entry.getValue().getA().run();
                if (entry.getValue().getB().terminated) expiredActions.add(entry.getKey()); // Collect internal uuid
            }
        }
       /* this.uuidSpecificDelayedActionTimers.entrySet().stream().flatMap(entry -> entry.getValue().entrySet().stream().map(e -> new Tuple3<>(entry.getKey(), e.getKey(), e.getValue())))
            .forEach(entry -> {     // A = key uuid; B = internal uuid; C = Tuple2{action, timer}
            entry.getC().getB().tick();
            if (entry.getC().getB().isTerminated() || entry.getC().getB().isJustFinishedALoop()) {
                entry.getC().getA().accept(entry.getA());
                if (entry.getC().getB().terminated) expiredIdSpecificActions.add(new Tuple2<>(entry.getA(), entry.getB())); // Collect {keyUUID, internalUUID}
            }
        });*/
        expiredKeys.forEach(this.generalTimers::remove);
        expiredIdSpecificKeys.forEach(keys -> this.uuidSpecificTimers.remove(keys.row(), keys.column()));
        expiredActions.forEach(this.delayedActionTimers::remove);
        /*expiredIdSpecificActions.forEach(keys -> {
            if (this.uuidSpecificDelayedActionTimers.containsKey(keys.getA()))
                this.uuidSpecificDelayedActionTimers.get(keys.getA()).remove(keys.getB());
        });*/
        expiredKeys.clear();
        expiredIdSpecificKeys.clear();
        expiredActions.clear();
        //expiredIdSpecificActions.clear();
    }

    public boolean shouldTick() {
        return !this.generalTimers.isEmpty() || !this.uuidSpecificTimers.isEmpty() || !this.delayedActionTimers.isEmpty()/* || !this.uuidSpecificDelayedActionTimers.isEmpty()*/;
    }

    // General timers

    public void addTimer(String name, int ticks, int loopCount, boolean serialize) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be positive");
        if (loopCount <= 0) throw new IllegalArgumentException("Loop count must be positive");
        //if (name.equals("__uuidSpecific")) throw new IllegalArgumentException("\"__uuidSpecific\" is reserved and cannot use as a name.");
        this.generalTimers.put(name, new Timer(ticks, false, loopCount, serialize));
    }

    public void addTimer(String name, int ticks, boolean serialize) {
        this.addTimer(name, ticks, 1, serialize);
    }

    public void addInfiniteLoopTimer(String name, int periodTicks, boolean serialize) {
        if (periodTicks <= 0) throw new IllegalArgumentException("Period must be positive");
        //if (name.equals("__uuidSpecific")) throw new IllegalArgumentException("\"__uuidSpecific\" is reserved and cannot use as a name.");
        this.generalTimers.put(name, new Timer(periodTicks, true, 1, serialize));
    }

    public Optional<Timer> getGeneralTimer(String name) {
        return Optional.ofNullable(this.generalTimers.get(name)).filter(t -> !t.isTerminated());
    }

    public boolean hasGeneralTimer(String name) {
        return getGeneralTimer(name).isPresent();
    }

    public Optional<Timer> removeGeneralTimer(String name, boolean doTerminationActions) {
        Timer removed = this.generalTimers.remove(name);
        if (removed != null && doTerminationActions) {
            // Notify this
            this.onGeneralTimerExpire(name, true);
            // Notify entity class if using the access interface
            if (this.getEntity() instanceof IEntityTimerComponentAccess user) {
                user.onTimerExpire(this, name, true, null);
                if (this.isDefaultTimerComponent())
                    user.onDefaultTimerExpire(name, true, null);
            }
            // Notify event listeners
            MinecraftForge.EVENT_BUS.post(new ExpireEvent(this.getEntity(), this, name, true, null));
        }
        return Optional.ofNullable(removed);
    }

    public Optional<Timer> removeGeneralTimer(String name) {
        return this.removeGeneralTimer(name, false);
    }

    public List<String> getAllGeneralTimerNames() {
        return this.generalTimers.keySet().stream().toList();
    }

    /**
     * Action when a general (non-UUID-specific) timer expires.
     * @param terminated Whether this timer is terminated (finished all loops and to be removed).
     */
    @ApiStatus.OverrideOnly
    public void onGeneralTimerExpire(String name, boolean terminated) {}

    // UUID-specific timers

    public void addUUIDSpecificTimer(UUID uuid, String name, int ticks, int loopCount, boolean serialize) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be positive");
        if (loopCount <= 0) throw new IllegalArgumentException("Loop count must be positive");
        this.uuidSpecificTimers.put(uuid, name, new Timer(ticks, false, loopCount, serialize));
    }

    public void addUUIDSpecificInfiniteLoopTimer(UUID uuid, String name, int periodTicks, boolean serialize) {
        if (periodTicks <= 0) throw new IllegalArgumentException("Period must be positive");
        this.uuidSpecificTimers.put(uuid, name, new Timer(periodTicks, true, 1, serialize));
    }

    public Optional<Timer> getUUIDSpecificTimer(UUID uuid, String name) {
        return this.uuidSpecificTimers.get(uuid, name).filter(t -> !t.isTerminated());
    }

    public Optional<Timer> removeUUIDSpecificTimer(UUID uuid, String name) {
        return this.uuidSpecificTimers.remove(uuid, name);
    }

    public boolean hasUUIDSpecificTimer(UUID uuid, String name) {
        return getUUIDSpecificTimer(uuid, name).isPresent();
    }

    public List<String> getAllUUIDSpecificTimerNames(UUID uuid) {
        return this.uuidSpecificTimers.getRow(uuid).keySet().stream().toList();
    }

    public List<ITable2D.KeyPair<UUID, String>> getAllUUIDSpecificTimerNames() {
        return this.uuidSpecificTimers.entryStream().map(ITable2D.Entry::keyPair).toList();
    }



    // Delayed actions

    /**
     * Add a delayed-action timer.
     * @param action Action to invoke.
     * @param ticks Period in ticks for each loop.
     * @param loopCount Overall loops before termination.
     * @param infiniteLoop If true, the timer will keep running and never terminate until manually removed.
     * @param runImmediately If true, the action will be invoked immediately. Note that this option will not reduce the loop amount, and the action will be invoked for overall n+1 times.
     * @return A randomly-generated UUID for accessing this timer.
     */
    public UUID addDelayedAction(Runnable action, int ticks, int loopCount, boolean infiniteLoop, boolean runImmediately) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be positive");
        if (loopCount <= 0) throw new IllegalArgumentException("Loop count must be positive");
        UUID id = UUID.randomUUID();
        this.delayedActionTimers.put(id, new Tuple2<>(action, new Timer(ticks, infiniteLoop, loopCount, false)));
        if (runImmediately) {
            action.run();
        }
        return id;
    }

    @Deprecated
    public Optional<Timer> getActionTimer(Runnable action) {
        return this.delayedActionTimers.values().stream().filter(entry -> Objects.equals(action, entry.getA())).map(Tuple2::getB).findAny();
    }

    /**
     * Get the delayed action by ID. The ID is provided from {@code addDelayedAction}.
     */
    public Optional<Tuple2<Runnable, Timer>> getDelayedAction(UUID id) {
        return Optional.ofNullable(this.delayedActionTimers.get(id));
    }

    @Deprecated
    public boolean hasActionTimer(Runnable action) {
        return this.delayedActionTimers.values().stream().anyMatch(entry -> Objects.equals(action, entry.getA()));
    }

    /**
     * Check if the delayed action of ID is present. The ID is provided from {@code addDelayedAction}.
     */
    public boolean hasActionTimer(UUID id) {
        return this.delayedActionTimers.containsKey(id);
    }

    public Optional<Timer> removeActionTimer(Runnable action) {
        UUID internalID = this.delayedActionTimers.entrySet().stream().filter(entry -> Objects.equals(action, entry.getValue().getA()))
            .map(Map.Entry::getKey).findAny().orElse(null);
        if (internalID != null) {
            return Optional.ofNullable(this.delayedActionTimers.remove(internalID)).map(Tuple2::getB);
        }
        else return Optional.empty();
    }

    public Optional<Tuple2<Runnable, Timer>> removeActionTimer(UUID id) {
        return Optional.ofNullable(this.delayedActionTimers.remove(id));
    }
   /*
    // UUID-specific actions

    public UUID addUUIDSpecificDelayedAction(UUID uuid, Consumer<UUID> action, int ticks, int loopCount, boolean infiniteLoop, boolean runImmediately) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be positive");
        if (loopCount <= 0) throw new IllegalArgumentException("Loop count must be positive");
        UUID internalId = UUID.randomUUID();
        this.uuidSpecificDelayedActionTimers.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(internalId, new Tuple2<>(action, new Timer(ticks, infiniteLoop, loopCount, false)));
        if (runImmediately) {
            Timer timer = this.uuidSpecificDelayedActionTimers.get(uuid).get(internalId).getB();
            timer.ticksRemaining = 1;
            timer.loopCountRemaining++;
        }

    }

    @Deprecated
    public Optional<Timer> getUUIDSpecificDelayedActionTimer(UUID uuid, Consumer<UUID> action) {
        return Optional.ofNullable(this.uuidSpecificDelayedActionTimers.get(uuid))
                .flatMap(m -> m.values().stream().filter(v -> Objects.equals(v.getA(), action)).findAny())
                .map(Tuple2::getB);
    }

    @Deprecated
    public Optional<Timer> removeUUIDSpecificDelayedAction(UUID uuid, Consumer<UUID> action) {
        return this.uuidSpecificDelayedActionTimers.remove(uuid, action);
    }

    public boolean hasUUIDSpecificDelayedAction(UUID uuid, Consumer<UUID> action) {
        return getUUIDSpecificDelayedActionTimer(uuid, action).isPresent();
    }

    public List<Consumer<UUID>> getAllUUIDSpecificActions(UUID uuid) {
        return this.uuidSpecificDelayedActionTimers.getRow(uuid).keySet().stream().toList();
    }

    public List<ITable2D.KeyPair<UUID, Consumer<UUID>>> getAllUUIDSpecificActions() {
        return this.uuidSpecificDelayedActionTimers.entryStream().map(ITable2D.Entry::keyPair).toList();
    }
*/
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();

        CompoundTag general = new CompoundTag();
        this.generalTimers.entrySet().stream().filter(entry -> entry.getValue().shouldSerialize())
            .forEach(entry -> general.put(entry.getKey(), entry.getValue().serialize()));
        nbt.put("generalTimers", general);

        ListTag uuidSpecific = new ListTag();
        this.uuidSpecificTimers.entryStream().filter(entry -> entry.value().shouldSerialize())
                .forEach(entry -> {
            CompoundTag entryNBT = new CompoundTag();
            entryNBT.putUUID("uuid", entry.rowKey());
            entryNBT.putString("name", entry.columnKey());
            entryNBT.put("timer", entry.value().serialize());
            uuidSpecific.add(entryNBT);
        });
        nbt.put("uuidSpecificTimers", uuidSpecific);
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        // Port legacy to new format first
        CompoundTag nbtPorted;
        if (nbt.contains("__uuidSpecific", Tag.TAG_LIST)) {
            nbtPorted = new CompoundTag();
            CompoundTag general = new CompoundTag();
            nbt.getAllKeys().stream().filter(k -> !k.equals("__uuidSpecific") && nbt.get(k) != null).forEach(k -> general.put(k, nbt.get(k).copy()));
            nbtPorted.put("generalTimers", general);
            nbtPorted.put("uuidSpecificTimers", nbt.getList("__uuidSpecific", Tag.TAG_COMPOUND).copy());
        }
        else nbtPorted = nbt;

        CompoundTag general = nbtPorted.getCompound("generalTimers");
        general.getAllKeys().forEach(key -> this.generalTimers.put(key, Timer.deserialize(general.getCompound(key))));
        nbtPorted.getList("uuidSpecificTimers", Tag.TAG_COMPOUND).stream()
            .filter(tag -> tag instanceof CompoundTag)
            .map(tag -> (CompoundTag)tag)
            .forEach(tag -> this.uuidSpecificTimers.put(tag.getUUID("tag"), tag.getString("name"), Timer.deserialize(tag.getCompound("timer"))));
    }

    public static class Timer {
        // The time period length of each loop
        protected int setTimeTicks;
        // Current ticks until the next expiration
        protected int ticksRemaining;
        // Whether this timer should loop infinitely
        protected boolean infiniteLoop;
        // How many loops before this timer finally terminates
        protected int loopCountRemaining;
        // Whether this timer should be saved/loaded
        protected boolean shouldSerialize;

        // Whether this timer has terminated (expired and no more restarting loops)
        private boolean terminated = false;
        // Whether this timer just finished a loop and going to the next loop. True for only one tick each loop.
        private boolean justFinishedALoop = false;

        private Timer(int setTimeTicks, boolean infiniteLoop, int loopCountRemaining, boolean shouldSerialize) {
            this.setTimeTicks = setTimeTicks;
            this.ticksRemaining = setTimeTicks;
            this.infiniteLoop = infiniteLoop;
            this.loopCountRemaining = loopCountRemaining;
            this.shouldSerialize = shouldSerialize;
        }

        public void tick() {
            if (this.terminated) return;
            if (this.justFinishedALoop) justFinishedALoop = false;
            --ticksRemaining;
            if (ticksRemaining <= 0) {
                if (loopCountRemaining <= 0) { // This should not happen
                    this.terminated = true;
                    return;
                }
                if (infiniteLoop) {
                    ticksRemaining = setTimeTicks;
                    this.justFinishedALoop = true;
                } // Infinite = always loop
                else {
                    --loopCountRemaining;
                    if (loopCountRemaining > 0) { // This means loops are not running out
                        ticksRemaining = setTimeTicks;
                        return;
                    } else {
                        this.terminated = true;
                        return;
                    }
                }
            }
        }

        public boolean isTerminated() {
            return this.terminated;
        }

        public boolean isJustFinishedALoop() {
            return this.justFinishedALoop;
        }

        public CompoundTag serialize() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("setTimeTicks", this.setTimeTicks);
            nbt.putInt("ticksRemaining", this.ticksRemaining);
            nbt.putBoolean("infiniteLoop", this.infiniteLoop);
            nbt.putInt("loopCountRemaining", this.loopCountRemaining);
            nbt.putBoolean("terminated", this.terminated);
            nbt.putBoolean("justFinishedALoop", this.justFinishedALoop);
            return nbt;
        }

        public static Timer deserialize(CompoundTag nbt) {
            Timer res = new Timer(nbt.getInt("setTimeTicks"), nbt.getBoolean("infiniteLoop"),
                nbt.getInt("loopCountRemaining"), true);
            res.ticksRemaining = nbt.getInt("ticksRemaining");
            res.terminated = nbt.getBoolean("expired") /* TODO legacy */ || nbt.getBoolean("terminated");
            res.justFinishedALoop = nbt.getBoolean("justFinishedALoop");
            return res;
        }

        public int getSetTimeTicks() {
            return setTimeTicks;
        }

        public int getTicksRemaining() {
            return ticksRemaining;
        }

        public boolean isInfiniteLoop() {
            return infiniteLoop;
        }

        public int getLoopCountRemaining() {
            return loopCountRemaining;
        }

        public boolean shouldSerialize() {
            return shouldSerialize;
        }

        /**
         * Force set this timer terminated. Only invoked when manually removing a timer.
         */
        @ApiStatus.Internal
        public void terminate() {
            this.ticksRemaining = 0;
            this.loopCountRemaining = 0;
            this.terminated = true;
        }
    }

    public static class ExpireEvent extends NFUEntityEvent<Entity> {

        private final EntityTimerComponent<? extends Entity> component;
        private final String name;
        private final boolean terminated;
        @Nullable
        private final UUID uuid;

        public ExpireEvent(Entity entity, EntityTimerComponent<? extends Entity> component, String name, boolean terminated, @Nullable UUID uuid) {
            super(entity);
            this.name = name;
            this.component = component;
            this.terminated = terminated;
            this.uuid = uuid;
        }

        public String getName() {return name;}

        /**
         * Whether this timer is terminated (expired and finished all loops, to be removed). False if the timer finished a loop and is entering the next loop.
         */
        public boolean isTerminated() {
            return terminated;
        }

        public boolean isUUIDSpecific() {
            return this.uuid != null;
        }

        public Optional<UUID> getUUID() {
            return Optional.ofNullable(uuid);
        }

        public EntityTimerComponent<? extends Entity> getComponent() {
            return component;
        }
    }

}
