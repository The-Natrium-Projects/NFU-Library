package net.sodiumzh.nfu.item.bauble;

import net.neoforged.eventbus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Fired on registering bauble items. Bauble items are only allowed to register here.
 */
public class RegisterBaublesEvent extends Event implements IModBusEvent
{
	public void register(IBaubleRegistryEntry entry)
	{
		BaubleRegistries.registerBaubleRaw(entry);
	}
}
