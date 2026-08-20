package net.sodiumzh.nfu.item.bauble;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.event.AttachCapabilitiesEvent;
import net.neoforged.event.entity.living.LivingEvent.LivingTickEvent;
import net.neoforged.eventbus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.annotation.DontCallManually;

@Mod.EventBusSubscriber(modid = NFULibrary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
class BaubleSystemEventListeners
{

	@SuppressWarnings("unchecked")
	@DontCallManually
	@SubscribeEvent
	public static void attachLivingEntityCapabilities(AttachCapabilitiesEvent<Entity> event)
	{
		if (event.getObject() instanceof Mob mob)
		{
			if (BaubleEquippableMobRegistries.containsMobType(mob.getClass()))
			{
				CBaubleEquippableMobPrvd prvd = new CBaubleEquippableMobPrvd(mob);
				event.addCapability(new ResourceLocation(NFULibrary.MOD_ID, "cap_bauble_equippable_mob"), prvd);
			}
		}
	}

	@SubscribeEvent
	public static void onLivingTick(LivingTickEvent event)
	{
		event.getEntity().getCapability(BaubleSystemCapabilities.CAP_BAUBLE_EQUIPPABLE_MOB)
			.filter(cap -> cap.getMob().tickCount % cap.getTickInterval() == 0)
			.ifPresent(CBaubleEquippableMob::tick);
	}
}
