package net.sodiumzh.nfu.item.bauble;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiPredicate;

/**
 * Record describing a registry record of a bauble.
 */
public class BaubleRecord {

    public static record SingleItem(Item item, double priority, IBaubleRegistryEntry bauble, BaubleEquippingCondition equippingCondition){}

    public static record MultiItem(BiPredicate<Item, ItemStack> predicate, IBaubleRegistryEntry bauble, BaubleEquippingCondition equippingCondition){}

}
