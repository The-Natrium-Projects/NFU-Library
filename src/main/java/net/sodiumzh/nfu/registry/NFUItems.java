package net.sodiumzh.nfu.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.item.debug.DebugAISwitchItem;
import net.sodiumzh.nfu.item.debug.DebugMobRemoverItem;
import net.sodiumzh.nfu.item.debug.DebugTargetSetterItem;
import net.sodiumzh.nfu.item.debug.TagDisplayerItem;

public class NFUItems
{
	public static final DeferredRegister<Item> NAUTILS_ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, NFULibrary.MOD_ID);
	
	public static final DeferredHolder<Item, DebugAISwitchItem> DEBUG_AI_SWITCH = NAUTILS_ITEMS.register("debug_ai_switch",
			() -> new DebugAISwitchItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
					.descTranslatable("info.nfulib.item.debug_ai_switch_desc")
					.cast());

	public static final DeferredHolder<Item, DebugTargetSetterItem> DEBUG_TARGET_SETTER = NAUTILS_ITEMS.register("debug_target_setter",
			() -> new DebugTargetSetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
					.descTranslatable("info.nfulib.item.debug_target_setter_desc")
					.cast());

	public static final DeferredHolder<Item, DebugMobRemoverItem> DEBUG_MOB_REMOVER = NAUTILS_ITEMS.register("debug_mob_remover",
			() -> new DebugMobRemoverItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
					.descTranslatable("info.nfulib.item.debug_mob_remover_desc")
					.description(DebugMobRemoverItem::getModeInfo)
					.description(DebugMobRemoverItem::getModeDesc)
					.descTranslatable("info.nfulib.item.debug_mob_remover_switch_mode")
					.cast());

	public static final DeferredHolder<Item, TagDisplayerItem> TAG_DISPLAYER = NAUTILS_ITEMS.register("tag_displayer",
		() -> new TagDisplayerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
}
