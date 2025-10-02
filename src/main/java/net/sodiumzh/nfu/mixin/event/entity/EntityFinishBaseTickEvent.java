package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

/**
 * Posted inside {@link Entity#tick} after {@link Entity#baseTick}.
 * NOT cancellable.
 */
public class EntityFinishBaseTickEvent extends NFUEntityEvent<Entity>
{

    public EntityFinishBaseTickEvent(Entity entity) {
        super(entity);
    }
}
