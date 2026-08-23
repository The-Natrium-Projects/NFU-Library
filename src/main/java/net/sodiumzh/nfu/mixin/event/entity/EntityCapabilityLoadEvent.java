package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

public class EntityCapabilityLoadEvent extends NFUEntityEvent<Entity> {
    public EntityCapabilityLoadEvent(Entity entity) {
        super(entity);
    }
}
