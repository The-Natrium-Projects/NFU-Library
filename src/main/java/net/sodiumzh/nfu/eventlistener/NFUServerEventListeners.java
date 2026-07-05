package net.sodiumzh.nfu.eventlistener;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.capability.CEntityTickingCapability;
import net.sodiumzh.nfu.entity.ConditionalAttributeModifier;
import net.sodiumzh.nfu.mixin.event.entity.EntityStartBaseTickEvent;
import net.sodiumzh.nfu.mixin.event.entity.EntityStartTickEvent;
import net.sodiumzh.nfu.registry.NFURegistry;
import net.sodiumzh.nfu.registry.NFURegistryGenerateValuesEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = NFULibrary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NFUServerEventListeners {

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent event)
	{
		if (event.phase == Phase.START)
			ConditionalAttributeModifier.update();
	}

	@SubscribeEvent
	public static void onServerStart(ServerStartingEvent event)
	{
		NFURegistry.SERVER_SETUP_DONE.trySet(true);
		List<NFURegistry<?>> shouldGenerate = NFURegistry.allRegistries().values().stream()
				.filter(reg -> reg.isAvailableOnServer() && reg.getLoadTiming().equals(NFURegistry.LoadTiming.SIDE_SETUP))
				.toList();
		shouldGenerate = NFURegistry.sortByLoadingOrder(shouldGenerate);
		shouldGenerate.forEach(reg -> MinecraftForge.EVENT_BUS.post(new NFURegistryGenerateValuesEvent.ServerBefore(reg, event.getServer())));
		shouldGenerate.forEach(NFURegistry::load);
		shouldGenerate.forEach(reg -> MinecraftForge.EVENT_BUS.post(new NFURegistryGenerateValuesEvent.ServerAfter(reg, event.getServer())));
	}

	@SubscribeEvent
	public static void onEntityTick(EntityStartBaseTickEvent event) {
		CEntityTickingCapability.ALL_CAPS.forEach(c -> event.getEntity().getCapability(c).ifPresent(CEntityTickingCapability::tick));
	}
}
