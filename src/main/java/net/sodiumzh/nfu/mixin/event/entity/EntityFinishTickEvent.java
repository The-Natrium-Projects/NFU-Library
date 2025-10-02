package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

/**
 * Posted after {@link Entity#tick}.
 * NOT cancellable.
 * <p>Note: this event is only posted in vanilla {@link Entity#tick} call. Manual call
 * of {@link Entity#tick} in other mods will not post this event.
 */
public class EntityFinishTickEvent extends NFUEntityEvent<Entity>
{

    public EntityFinishTickEvent(Entity entity) {
        super(entity);
    }
}
