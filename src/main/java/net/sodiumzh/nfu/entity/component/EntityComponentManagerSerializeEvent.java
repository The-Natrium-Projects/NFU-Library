package net.sodiumzh.nfu.entity.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

import java.util.List;

public abstract class EntityComponentManagerSerializeEvent extends NFUEntityEvent<Entity> {

    private final CompoundTag nbt;

    public EntityComponentManagerSerializeEvent(Entity entity, CompoundTag nbt) {
        super(entity);
        this.nbt = nbt;
    }

    public CEntityComponentManager getManager() {
        return EntityComponentAPI.getComponentManager(this.getEntity());
    }

    /**
     * The NBT to be serialized. In {@link EntityComponentManagerSerializeEvent.Before} it's always empty initially.
     * <p>See {@link CEntityComponentManagerImpl} for data format.
     */
    public CompoundTag getNBT() {
        return nbt;
    }

    /**
     * Posted before component manager serialization.
     */
    public static class Before extends EntityComponentManagerSerializeEvent {
        public Before(Entity entity, CompoundTag nbt) {
            super(entity, nbt);
        }
    }

    /**
     * Posted after component manager serialization.
     */
    public static class After extends EntityComponentManagerSerializeEvent {
        public After(Entity entity, CompoundTag nbt) {
            super(entity, nbt);
        }
    }

}

