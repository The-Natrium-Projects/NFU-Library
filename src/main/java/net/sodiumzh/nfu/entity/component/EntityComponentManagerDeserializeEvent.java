package net.sodiumzh.nfu.entity.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

public abstract class EntityComponentManagerDeserializeEvent extends NFUEntityEvent<Entity> {

    private final CompoundTag nbt;

    public EntityComponentManagerDeserializeEvent(Entity entity, CompoundTag nbt) {
        super(entity);
        this.nbt = nbt;
    }

    public CEntityComponentManager getManager() {
        return EntityComponentAPI.getComponentManager(this.getEntity());
    }

    public CompoundTag getNBT() {
        return nbt;
    }

    public static class Before extends EntityComponentManagerDeserializeEvent {
        public Before(Entity entity, CompoundTag nbt) {
            super(entity, nbt);
        }
    }

    public static class After extends EntityComponentManagerDeserializeEvent {
        public After(Entity entity, CompoundTag nbt) {
            super(entity, nbt);
        }
    }

}

