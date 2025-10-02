package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

/**
 * Posted inside {@link Entity#tick} before {@link Entity#baseTick}.
 * NOT cancellable.
 */
public class EntityStartBaseTickEvent extends NFUEntityEvent<Entity>
{

    public EntityStartBaseTickEvent(Entity entity) {
        super(entity);
    }
}
