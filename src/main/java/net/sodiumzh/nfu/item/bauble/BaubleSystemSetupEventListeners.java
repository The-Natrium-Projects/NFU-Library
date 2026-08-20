package net.sodiumzh.nfu.item.bauble;

import net.neoforged.common.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.eventbus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.sodiumzh.nfu.NFULibrary;

@Mod.EventBusSubscriber(modid = NFULibrary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
class BaubleSystemSetupEventListeners
{

	@SubscribeEvent
	public static void baubleSystemSetup(FMLCommonSetupEvent event)
	{
		event.enqueueWork(() -> 
		{
			ModLoader.get().postEvent(new RegisterBaublesEvent());
			BaubleRegistries.clearAndFillRegistries();
			ModLoader.get().postEvent(new ModifyBaubleRegistriesEvent());
			BaubleRegistries.sortSingleRegistry();
			ModLoader.get().postEvent(new RegisterBaubleEquippableMobsEvent());
			ModLoader.get().postEvent(new ModifyBaubleEquippableMobsEvent());
		});
	}

	@SubscribeEvent
	public static void register(RegisterCapabilitiesEvent event)
	{
		event.register(CBaubleEquippableMob.class);
	}
}
