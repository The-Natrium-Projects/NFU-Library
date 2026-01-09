package net.sodiumzh.nfu.entity.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.event.NFUEntityEvent;

import java.util.*;

public abstract class EntityTimerComponent extends EntityComponentBase {

    private final Map<String, Timer> namedTimers = new HashMap<>();
    private final Map<Runnable, Timer> delayedActionTimers = new HashMap<>();

    List<String> deadKeys = new ArrayList<>();  // Temporary list, only works on tick
    List<Runnable> deadActions = new ArrayList<>();   // Temporary list, only works on tick

    public EntityTimerComponent(Entity entity) {
        super(entity);
    }

    public void tick() {
        for (var entry: namedTimers.entrySet()) {
            entry.getValue().tick();
            if (entry.getValue().isDead() || entry.getValue().isJustFinishedALoop()) {
                if (this.getEntity() instanceof IEntityTimerComponentHolder holder)
                    holder.onTimerExpire(entry.getKey(), entry.getValue().isDead());
                MinecraftForge.EVENT_BUS.post(new ExpireEvent(this.getEntity(), entry.getKey(), entry.getValue().isDead()));
                if (entry.getValue().isDead()) deadKeys.add(entry.getKey());
            }
        }
        for (var entry: delayedActionTimers.entrySet()) {
            entry.getValue().tick();
            if (entry.getValue().isDead() || entry.getValue().isJustFinishedALoop()) {
                entry.getKey().run();
                if (entry.getValue().dead) deadActions.add(entry.getKey());
            }
        }
        deadKeys.forEach(this.namedTimers::remove);
        deadActions.forEach(this.delayedActionTimers::remove);
        deadKeys.clear();
        deadActions.clear();
    }

    public void addTimer(String name, int ticks, int loopCount, boolean serialize) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be positive");
        if (loopCount <= 0) throw new IllegalArgumentException("Loop count must be positive");
        this.namedTimers.put(name, new Timer(ticks, false, loopCount, serialize));
    }

    public void addTimer(String name, int ticks, boolean serialize) {
        this.addTimer(name, ticks, 1, serialize);
    }

    public void addInfiniteLoopTimer(String name, int periodTicks, boolean serialize) {
        if (periodTicks <= 0) throw new IllegalArgumentException("Period must be positive");
        this.namedTimers.put(name, new Timer(periodTicks, true, 1, serialize));
    }

    public Optional<Timer> getNamedTimer(String name) {
        return Optional.ofNullable(this.namedTimers.get(name)).filter(t -> !t.isDead());
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

    public List<Runnable> getAllDelayedActions() {
        return this.delayedActionTimers.keySet().stream().toList();0

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

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        this.namedTimers.entrySet().stream().filter(entry -> entry.getValue().shouldSerialize())
            .forEach(entry -> nbt.put(entry.getKey(), entry.getValue().serialize()));
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        nbt.getAllKeys().forEach(key -> this.namedTimers.put(key, Timer.deserialize(nbt.getCompound(key))));
    }

    public static class Timer {
        protected int setTimeTicks;
        protected int ticksRemaining;
        protected boolean infiniteLoop;
        protected int loopCountRemaining;
        protected boolean shouldSerialize;

        private boolean dead = false;
        private boolean justFinishedALoop = false;

        private Timer(int setTimeTicks, boolean infiniteLoop, int loopCountRemaining, boolean shouldSerialize) {
            this.setTimeTicks = setTimeTicks;
            this.ticksRemaining = setTimeTicks;
            this.infiniteLoop = infiniteLoop;
            this.loopCountRemaining = loopCountRemaining;
            this.shouldSerialize = shouldSerialize;
        }

        public void tick() {
            if (this.dead) return;
            if (this.justFinishedALoop) justFinishedALoop = false;
            --ticksRemaining;
            if (ticksRemaining <= 0) {
                if (loopCountRemaining <= 0) { // This should not happen
                    this.dead = true;
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
                        this.dead = true;
                        return;
                    }
                }
            }
        }

        public boolean isDead() {
            return this.dead;
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
            nbt.putBoolean("dead", this.dead);
            nbt.putBoolean("justFinishedALoop", this.justFinishedALoop);
            return nbt;
        }

        public static Timer deserialize(CompoundTag nbt) {
            Timer res = new Timer(nbt.getInt("setTimeTicks"), nbt.getBoolean("infiniteLoop"),
                nbt.getInt("loopCountRemaining"), true);
            res.ticksRemaining = nbt.getInt("ticksRemaining");
            res.dead = nbt.getBoolean("dead");
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
        private final boolean dying;

        public ExpireEvent(Entity entity, String name, boolean dying) {
            super(entity);
            this.name = name;
            this.dying = dying;
        }

        public String getName() {return name;}

        public boolean isTimerDying() {
            return dying;
        }
    }

}
