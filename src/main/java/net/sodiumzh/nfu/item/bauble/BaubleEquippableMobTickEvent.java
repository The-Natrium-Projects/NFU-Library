package net.sodiumzh.nfu.item.bauble;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.eventbus.api.Cancelable;
import net.sodiumzh.nfu.event.NFULivingEvent;


/**
 * Events posted during Bauble-equippable mob tick.
 * <p><b>Note: DO NOT directly listen to this event!</b> It will post 4 times each tick, 
 * including before-tick, before-slot-tick, after-slot-tick, after-tick successively. Listen to specific events instead.
 */
public abstract class BaubleEquippableMobTickEvent extends NFULivingEvent<Mob>
{
	public final CBaubleEquippableMob capability;
	
	public BaubleEquippableMobTickEvent(Mob mob, CBaubleEquippableMob cap)
	{
		super(mob);
		this.capability = cap;
	}

	/**
	 * Posted before slots pre-tick but after attribute modifiers update for Bauble-equippable mobs.
	 */
	public static class BeforeTick extends BaubleEquippableMobTickEvent
	{

		public BeforeTick(Mob mob, CBaubleEquippableMob cap)
		{
			super(mob, cap);
		}
	}
	
	/**
	 * Posted after slots pre-tick before slots tick for Bauble-equippable mobs.
	 */
	public static class BeforeSlotTick extends BaubleEquippableMobTickEvent
	{

		public BeforeSlotTick(Mob mob, CBaubleEquippableMob cap)
		{
			super(mob, cap);
		}
	}
	
	/**
	 * Posted after slots tick before slots post-tick for Bauble-equippable mobs.
	 */
	public static class AfterSlotTick extends BaubleEquippableMobTickEvent
	{

		public AfterSlotTick(Mob mob, CBaubleEquippableMob cap)
		{
			super(mob, cap);
		}

	}
	
	/**
	 * Posted on each slots ticks.
	 * <p>Cancellable. If cancelled, the slot tick will be omitted.
	 */
	@Cancelable
	public static class SlotTick extends BaubleEquippableMobTickEvent
	{

		public SlotTick(Mob mob, CBaubleEquippableMob cap)
		{
			super(mob, cap);
		}

	}
	
	/**
	 * Posted after slots post-tick for Bauble-equippable mobs.
	 */
	public static class AfterTick extends BaubleEquippableMobTickEvent
	{

		public AfterTick(Mob mob, CBaubleEquippableMob cap)
		{
			super(mob, cap);
		}
	}
}
