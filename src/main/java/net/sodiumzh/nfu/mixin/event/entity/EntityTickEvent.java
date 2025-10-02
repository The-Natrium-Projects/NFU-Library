package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

/**
 * Fired on the beginning of {@code Entity#tick}, before {@code Entity#baseTick}.
 * NOT cancellable.
 * @deprecated Use {@link EntityStartTickEvent}, {@link EntityFinishTickEvent},
 * {@link EntityStartBaseTickEvent} or {@link EntityFinishBaseTickEvent} instead.
 */
@Deprecated
public class EntityTickEvent extends NFUEntityEvent<Entity>
{
	public EntityTickEvent(Entity entity)
	{
		super(entity);
	}	
}
