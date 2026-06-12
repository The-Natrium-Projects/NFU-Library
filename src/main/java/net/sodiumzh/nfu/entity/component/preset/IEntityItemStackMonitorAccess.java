package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.world.item.ItemStack;

/**
 * A utility interface to operate {@link EntityAttributeMonitorComponent} in the entity class definition.
 */
public interface IEntityItemStackMonitorAccess extends IEntityComponentAccess {

    public void setupItemStackMonitor(EntityItemStackMonitorComponent component);

    public void onItemStackChange(EntityItemStackMonitorComponent source, String key, ItemStack oldItemStackCopy, ItemStack newItemStackCopy);

}
