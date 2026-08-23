package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

public class EntityCapabilityFinishLoadingEvent extends NFUEntityEvent<Entity> {
    public EntityCapabilityFinishLoadingEvent(Entity entity) {
        super(entity);
    }
}
