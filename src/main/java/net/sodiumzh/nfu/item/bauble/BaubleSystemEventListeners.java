package net.sodiumzh.nfu.item.bauble;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.annotation.DontCallManually;
import net.sodiumzh.nfu.mixin.event.entity.EntityCapabilityFinishLoadingEvent;
import net.sodiumzh.nfu.mixin.event.entity.EntityLoadEvent;

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

	@SubscribeEvent
	public static void onLivingCapabilityLoad(EntityCapabilityFinishLoadingEvent event) {
		if (event.getEntity() instanceof Mob mob) {
			// Load attribute modifiers before loading actual HP, so that HP will not be truncated
			NFUBaubleAPI.getOptionalCapability(mob).ifPresent(CBaubleEquippableMob::tick);
		}
	}
}
