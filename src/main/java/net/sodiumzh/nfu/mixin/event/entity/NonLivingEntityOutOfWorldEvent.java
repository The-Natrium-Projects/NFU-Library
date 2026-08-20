package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.Entity;
import net.neoforged.event.entity.EntityEvent;

public class NonLivingEntityOutOfWorldEvent extends EntityEvent
{
	public final float amount;
	
	public NonLivingEntityOutOfWorldEvent(Entity entity, float amount)
	{
		super(entity);
		this.amount = amount;
	}

}
