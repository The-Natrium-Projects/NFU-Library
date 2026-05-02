package net.sodiumzh.nfu.entity.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.container.ITable2D;
import net.sodiumzh.nfu.container.Table2D;
import net.sodiumzh.nfu.event.NFUEntityEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class EntityTimerComponent<T extends Entity> extends EntityComponentBase<T> {

    private final Map<String, Timer> namedTimers = new HashMap<>();
    private final ITable2D<UUID, String, Timer> uuidSpecificNamedTimers = new Table2D<>();
    private final Map<Runnable, Timer> delayedActionTimers = new HashMap<>();
    private final ITable2D<UUID, Consumer<UUID>, Timer> uuidSpecificDelayedActionTimers
            = new Table2D<>();

    private final List<String> expiredKeys = new ArrayList<>();  // Temporary list, only works on tick
    private final List<ITable2D.KeyPair<UUID, String>> expiredIdSpecificKeys = new ArrayList<>();
    private final List<Runnable> expiredActions = new ArrayList<>();   // Temporary list, only works on tick
    private final List<ITable2D.KeyPair<UUID, Consumer<UUID>>> expiredIdSpecificActions = new ArrayList<>();

    public EntityTimerComponent(T entity) {
        super(entity);
    }

    public final boolean isDefaultTimerComponent() {
        return this.equals(EntityComponentAPI.getDefaultTimer(this.getEntity()));
    }

    public void tick() {
        for (var entry: namedTimers.entrySet()) {
            entry.getValue().tick();
            if (entry.getValue().isExpired() || entry.getValue().isJustFinishedALoop()) {
                if (this.getEntity() instanceof IEntityTimerComponentUser user) {
                    user.onTimerExpire(this, entry.getKey(), entry.getValue().isExpired(), null);
                    if (this.isDefaultTimerComponent())
                        user.onDefaultTimerExpire(entry.getKey(), entry.getValue().isExpired(), null);
                }
                MinecraftForge.EVENT_BUS.post(new ExpireEvent(this.getEntity(), entry.getKey(), entry.getValue().isExpired(), null));
                if (entry.getValue().isExpired()) expiredKeys.add(entry.getKey());
            }
        }
        this.uuidSpecificNamedTimers.entryStream().forEach(entry -> {
            entry.value().tick();
            if (entry.value().isExpired() || entry.value().isJustFinishedALoop()) {
                if (this.getEntity() instanceof IEntityTimerComponentUser user) {
                    user.onTimerExpire(this, entry.columnKey(), entry.value().isExpired(), entry.rowKey());
                    if (this.isDefaultTimerComponent())
                        user.onDefaultTimerExpire(entry.columnKey(), entry.value().isExpired(), entry.rowKey());
                }
                MinecraftForge.EVENT_BUS.post(new ExpireEvent(this.getEntity(), entry.columnKey(), entry.value().isExpired(), entry.rowKey()));
                if (entry.value().isExpired()) expiredIdSpecificKeys.add(new ITable2D.KeyPair<>(entry.rowKey(), entry.columnKey()));
            }
        });
        for (var entry: delayedActionTimers.entrySet()) {
            entry.getValue().tick();
            if (entry.getValue().isExpired() || entry.getValue().isJustFinishedALoop()) {
                entry.getKey().run();
                if (entry.getValue().expired) expiredActions.add(entry.getKey());
            }
        }
        this.uuidSpecificDelayedActionTimers.entryStream().forEach(entry -> {
            entry.value().tick();
            if (entry.value().isExpired() || entry.value().isJustFinishedALoop()) {
                entry.columnKey().accept(entry.rowKey());
                if (entry.value().expired) expiredIdSpecificActions.add(new ITable2D.KeyPair<>(entry.rowKey(), entry.columnKey()));
            }
        });
        expiredKeys.forEach(this.namedTimers::remove);
        expiredIdSpecificKeys.forEach(keys -> this.uuidSpecificNamedTimers.remove(keys.row(), keys.column()));
        expiredActions.forEach(this.delayedActionTimers::remove);
        expiredIdSpecificActions.forEach(keys -> this.uuidSpecificDelayedActionTimers.remove(keys.row(), keys.column()));
        expiredKeys.clear();
        expiredIdSpecificKeys.clear();
        expiredActions.clear();
        expiredIdSpecificActions.clear();
    }

    // General timers

    public void addTimer(String name, int ticks, int loopCount, boolean serialize) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be positive");
        if (loopCount <= 0) throw new IllegalArgumentException("Loop count must be positive");
        if (name.equals("__uuidSpecific")) throw new IllegalArgumentException("\"__uuidSpecific\" is reserved and cannot use as a name.");
        this.namedTimers.put(name, new Timer(ticks, false, loopCount, serialize));
    }

    public void addTimer(String name, int ticks, boolean serialize) {
        this.addTimer(name, ticks, 1, serialize);
    }

    public void addInfiniteLoopTimer(String name, int periodTicks, boolean serialize) {
        if (periodTicks <= 0) throw new IllegalArgumentException("Period must be positive");
        if (name.equals("__uuidSpecific")) throw new IllegalArgumentException("\"__uuidSpecific\" is reserved and cannot use as a name.");
        this.namedTimers.put(name, new Timer(periodTicks, true, 1, serialize));
    }

    public Optional<Timer> getNamedTimer(String name) {
        return Optional.ofNullable(this.namedTimers.get(name)).filter(t -> !t.isExpired());
    }

    public boolean hasNamedTimer(String name) {
        return getNamedTimer(name).isPresent();
    }

    public Optional<Timer> removeNamedTimer(String name) {
        return Optional.ofNullable(this.namedTimers.remove(name));
    }

    public List<String> getAllTimerNames() {
        return this.namedTimers.keySet().stream().toList();
    }

    // UUID-specific timers

    public void addUUIDSpecificTimer(UUID uuid, String name, int ticks, int loopCount, boolean serialize) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be positive");
        if (loopCount <= 0) throw new IllegalArgumentException("Loop count must be positive");
        this.uuidSpecificNamedTimers.put(uuid, name, new Timer(ticks, false, loopCount, serialize));
    }

    public void addUUIDSpecificInfiniteLoopTimer(UUID uuid, String name, int periodTicks, boolean serialize) {
        if (periodTicks <= 0) throw new IllegalArgumentException("Period must be positive");
        this.uuidSpecificNamedTimers.put(uuid, name, new Timer(periodTicks, true, 1, serialize));
    }

    public Optional<Timer> getUUIDSpecificTimer(UUID uuid, String name) {
        return this.uuidSpecificNamedTimers.get(uuid, name).filter(t -> !t.isExpired());
    }

    public Optional<Timer> removeUUIDSpecificTimer(UUID uuid, String name) {
        return this.uuidSpecificNamedTimers.remove(uuid, name);
    }

    public boolean hasUUIDSpecificTimer(UUID uuid, String name) {
        return getUUIDSpecificTimer(uuid, name).isPresent();
    }

    public List<String> getAllUUIDSpecificTimerNames(UUID uuid) {
        return this.uuidSpecificNamedTimers.getRow(uuid).keySet().stream().toList();
    }

    public List<ITable2D.KeyPair<UUID, String>> getAllUUIDSpecificTimerNames() {
        return this.uuidSpecificNamedTimers.entryStream().map(ITable2D.Entry::keyPair).toList();
    }

    // Delayed actions

    public List<Runnable> getAllDelayedActions() {
        return this.delayedActionTimers.keySet().stream().toList();
    }

    public void addDelayedAction(Runnable action, int ticks, int loopCount, boolean infiniteLoop, boolean runImmediately) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be positive");
        if (loopCount <= 0) throw new IllegalArgumentException("Loop count must be positive");
        this.delayedActionTimers.put(action, new Timer(ticks, infiniteLoop, loopCount, false));
        if (runImmediately) {
            this.delayedActionTimers.get(action).ticksRemaining = 1;
            this.delayedActionTimers.get(action).loopCountRemaining++;
        }
    }

    public Optional<Timer> getActionTimer(Runnable action) {
        return Optional.ofNullable(this.delayedActionTimers.get(action));
    }

    public boolean hasActionTimer(Runnable action) {
        return getActionTimer(action).isPresent();
    }

    public Optional<Timer> removeActionTimer(Runnable action) {
        return Optional.ofNullable(this.delayedActionTimers.remove(action));
    }

    // UUID-specific actions

    public void addUUIDSpecificDelayedAction(UUID uuid, Consumer<UUID> action, int ticks, int loopCount, boolean infiniteLoop, boolean runImmediately) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be positive");
        if (loopCount <= 0) throw new IllegalArgumentException("Loop count must be positive");
        this.uuidSpecificDelayedActionTimers.put(uuid, action, new Timer(ticks, infiniteLoop, loopCount, false));
        if (runImmediately) {
            this.uuidSpecificDelayedActionTimers.get(uuid, action).ifPresent(timer -> {
                timer.ticksRemaining = 1;
                timer.loopCountRemaining++;
            });
        }

    }

    public Optional<Timer> getUUIDSpecificDelayedActionTimer(UUID uuid, Consumer<UUID> action) {
        return this.uuidSpecificDelayedActionTimers.get(uuid, action);
    }

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

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        this.namedTimers.entrySet().stream().filter(entry -> entry.getValue().shouldSerialize())
            .forEach(entry -> nbt.put(entry.getKey(), entry.getValue().serialize()));
        ListTag uuidSpecific = new ListTag();
        this.uuidSpecificNamedTimers.entryStream().filter(entry -> entry.value().shouldSerialize())
                .forEach(entry -> {
            CompoundTag entryNBT = new CompoundTag();
            entryNBT.putUUID("uuid", entry.rowKey());
            entryNBT.putString("name", entry.columnKey());
            entryNBT.put("timer", entry.value().serialize());
            uuidSpecific.add(entryNBT);
        });
        nbt.put("__uuidSpecific", uuidSpecific);
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        nbt.getAllKeys().stream().filter(k -> !k.equals("__uuidSpecific"))
                .forEach(key -> this.namedTimers.put(key, Timer.deserialize(nbt.getCompound(key))));
        nbt.getList("__uuidSpecific", ListTag.TAG_COMPOUND).forEach(tag -> {
            if (tag instanceof CompoundTag entry) {
                this.uuidSpecificNamedTimers.put(entry.getUUID("uuid"), entry.getString("name"),
                        Timer.deserialize(entry.getCompound("timer")));
            }
        });
    }

    public static class Timer {
        protected int setTimeTicks;
        protected int ticksRemaining;
        protected boolean infiniteLoop;
        protected int loopCountRemaining;
        protected boolean shouldSerialize;

        private boolean expired = false;
        private boolean justFinishedALoop = false;

        private Timer(int setTimeTicks, boolean infiniteLoop, int loopCountRemaining, boolean shouldSerialize) {
            this.setTimeTicks = setTimeTicks;
            this.ticksRemaining = setTimeTicks;
            this.infiniteLoop = infiniteLoop;
            this.loopCountRemaining = loopCountRemaining;
            this.shouldSerialize = shouldSerialize;
        }

        public void tick() {
            if (this.expired) return;
            if (this.justFinishedALoop) justFinishedALoop = false;
            --ticksRemaining;
            if (ticksRemaining <= 0) {
                if (loopCountRemaining <= 0) { // This should not happen
                    this.expired = true;
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
                        this.expired = true;
                        return;
                    }
                }
            }
        }

        public boolean isExpired() {
            return this.expired;
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
            nbt.putBoolean("expired", this.expired);
            nbt.putBoolean("justFinishedALoop", this.justFinishedALoop);
            return nbt;
        }

        public static Timer deserialize(CompoundTag nbt) {
            Timer res = new Timer(nbt.getInt("setTimeTicks"), nbt.getBoolean("infiniteLoop"),
                nbt.getInt("loopCountRemaining"), true);
            res.ticksRemaining = nbt.getInt("ticksRemaining");
            res.expired = nbt.getBoolean("expired");
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
    }

    public static class ExpireEvent extends NFUEntityEvent<Entity> {

        private final String name;
        private final boolean expiring;
        @Nullable
        private final UUID uuid;

        public ExpireEvent(Entity entity, String name, boolean expiring, @Nullable UUID uuid) {
            super(entity);
            this.name = name;
            this.expiring = expiring;
            this.uuid = uuid;
        }

        public String getName() {return name;}

        public boolean isTimerExpiring() {
            return expiring;
        }

        public boolean isUUIDSpecific() {
            return this.uuid != null;
        }

        public Optional<UUID> getUUID() {
            return Optional.ofNullable(uuid);
        }

    }

}
