package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

/**
 * Posted before {@link Entity#tick}.
 * NOT cancellable.
 * <p>Note: this event is only posted in vanilla {@link Entity#tick} call. Manual call
 * of {@link Entity#tick} in other mods will not post this event.
 */
public class EntityStartTickEvent extends NFUEntityEvent<Entity>
{

    public EntityStartTickEvent(Entity entity) {
        super(entity);
    }
}
