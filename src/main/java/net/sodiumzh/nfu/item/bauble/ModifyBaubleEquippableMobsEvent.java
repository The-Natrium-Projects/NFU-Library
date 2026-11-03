package net.sodiumzh.nfu.item.bauble;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

public class ModifyBaubleEquippableMobsEvent extends Event implements IModBusEvent
{

	/**
	 * Add a bauble entry for a special slot to accept.
	 */
	public void addSpecialSlotAccepted(EntityType<? extends Mob> type, String slot, ResourceLocation entryKey)
	{
		if (BaubleEquippableMobRegistries.isSlotSpecial(type, slot))
		{
			BaubleEquippableMobRegistries.getMobSlotPropertyMap(type).get(slot).d.add(BaubleRegistries.getEntryByKey(entryKey));
		}
		else LogUtils.getLogger().error("ModifyBaubleEquippableMobsEvent#addSpecialSlotAccepted: slot \"" + slot + "\" for mob class \""
				+ type.getDescriptionId() + "\" isn't a special slot or doesn't exist. Skipped.");
	}
	
	/**
	 * Add a bauble entry for a special slot to accept. Entry key uses ResourceLocation format.
	 */
	public void addSpecialSlotAccepted(EntityType<? extends Mob> type, String slot, String entryKey)
	{
		this.addSpecialSlotAccepted(type, slot, new ResourceLocation(entryKey));
	}
	
}
