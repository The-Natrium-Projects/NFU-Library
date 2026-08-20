package net.sodiumzh.nfu.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class NFUTagStatics
{
	public static boolean hasTag(Entity obj, ResourceLocation tag)
	{
		TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, tag);
		return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(obj.getType()).is(tagKey);
	}
	
	public static boolean hasTag(Entity obj, String tag)
	{
		return hasTag(obj, new ResourceLocation(tag));
	}
	
	public static boolean hasTag(Entity obj, String domain, String tag)
	{
		return hasTag(obj, new ResourceLocation(domain, tag));
	}
	
	public static boolean hasTag(Item obj, ResourceLocation tag)
	{
		TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tag);
		return BuiltInRegistries.ITEM.wrapAsHolder(obj).is(tagKey);
	}
	
	public static boolean hasTag(Item obj, String tag)
	{
		return hasTag(obj, new ResourceLocation(tag));
	}
	
	public static boolean hasTag(Item obj, String domain, String tag)
	{
		return hasTag(obj, new ResourceLocation(domain, tag));
	}

	public static boolean hasTag(ItemStack obj, ResourceLocation tag)
	{
		if (obj.isEmpty())
			return false;
		else return hasTag(obj.getItem(), tag);
	}
	
	public static boolean hasTag(ItemStack obj, String tag)
	{
		return hasTag(obj, new ResourceLocation(tag));
	}
	
	public static boolean hasTag(ItemStack obj, String domain, String tag)
	{
		return hasTag(obj, new ResourceLocation(domain, tag));
	}
	
	
	public static boolean hasTag(Block obj, ResourceLocation tag)
	{
		TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tag);
		return BuiltInRegistries.BLOCK.wrapAsHolder(obj).is(tagKey);
	}
	
	public static boolean hasTag(Block obj, String tag)
	{
		return hasTag(obj, new ResourceLocation(tag));
	}
	
	public static boolean hasTag(Block obj, String domain, String tag)
	{
		return hasTag(obj, new ResourceLocation(domain, tag));
	}

	
	public static ArrayList<EntityType<?>> getAllEntityTypesUnderTag(ResourceLocation tag)
	{
		TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, tag);
		ArrayList<EntityType<?>> res = new ArrayList<>();
		BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tagKey).forEach(holder -> res.add(holder.value()));
		return res;
	}
	
	public static ArrayList<EntityType<?>> getAllEntityTypesUnderTag(String tag)
	{
		return getAllEntityTypesUnderTag(new ResourceLocation(tag));
	}
	
	public static ArrayList<EntityType<?>> getAllEntityTypesUnderTag(String domain, String tag)
	{
		return getAllEntityTypesUnderTag(new ResourceLocation(domain, tag));
	}
	
	public static ArrayList<Item> getAllItemsUnderTag(ResourceLocation tag)
	{
		TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tag);
		ArrayList<Item> res = new ArrayList<>();
		BuiltInRegistries.ITEM.getTagOrEmpty(tagKey).forEach(holder -> res.add(holder.value()));
		return res;
	}
	
	public static ArrayList<Item> getAllItemsUnderTag(String tag)
	{
		return getAllItemsUnderTag(new ResourceLocation(tag));
	}
	
	public static ArrayList<Item> getAllItemsUnderTag(String domain, String tag)
	{
		return getAllItemsUnderTag(new ResourceLocation(domain, tag));
	}
	
	public static ArrayList<Block> getAllBlocksUnderTag(ResourceLocation tag)
	{
		TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tag);
		ArrayList<Block> res = new ArrayList<>();
		BuiltInRegistries.BLOCK.getTagOrEmpty(tagKey).forEach(holder -> res.add(holder.value()));
		return res;
	}
	
	public static ArrayList<Block> getAllBlocksUnderTag(String tag)
	{
		return getAllBlocksUnderTag(new ResourceLocation(tag));
	}
	
	public static ArrayList<Block> getAllBlocksUnderTag(String domain, String tag)
	{
		return getAllBlocksUnderTag(new ResourceLocation(domain, tag));
	}
	
	public static TagKey<Block> createBlockTag(String modId, String name)
	{
		return TagKey.create(Registries.BLOCK, new ResourceLocation(modId, name));
	}
	
	public static TagKey<Item> createItemTag(String modId, String name)
	{
		return TagKey.create(Registries.ITEM, new ResourceLocation(modId, name));
	}
	
	public static TagKey<EntityType<?>> createEntityTypeTag(String modId, String name)
	{
		return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(modId, name));
	}

	public static <T> List<TagKey<T>> getAllTags(T value, Registry<T> registry) {
		return registry.wrapAsHolder(value).tags().toList();
	}

}
