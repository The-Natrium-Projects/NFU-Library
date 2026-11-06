package net.sodiumzh.nfu.item.bauble;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.sodiumzh.nfu.container.Tuple4;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

class BaubleEquippableMobRegistries
{
	private static final HashMap<EntityType<? extends Mob>, Class<? extends Mob>>
		MOB_TYPE_TO_CLASS_MAPPING = new HashMap<>();

	/**
	 * Values: 
	 * <p>predicate - mob equippingCondition for having this slot;
	 * <p>function - ItemStack accessor; 
	 * <p>boolean - is special; 
	 * <p>set - only if special, items that can be equipped in. For non-special, it's null
	 * <p>Note: Special slot: a slot that's unable to equip anything unless Bauble registry entries are manually registered.
	 */
	 private static final HashMap<EntityType<? extends Mob>, Map<String, Tuple4<Predicate<Mob>, Function<Mob, ItemStack>, Boolean, Set<IBaubleRegistryEntry>>>>
		BAUBLE_EQUIPPABLE_MOBS = new HashMap<>();
	
	 // ============ Registration Methods =================
	 
	/**
	 * Register a mob without slot. If the mob exists, it won't do anything.
	 */
	static <T extends Mob> void registerMob(EntityType<? extends T> mobType, Class<T> mobClass)
	{
		updateTypeClassMapping(mobType, mobClass);
		if (!BAUBLE_EQUIPPABLE_MOBS.containsKey(mobType)) {
			BAUBLE_EQUIPPABLE_MOBS.put(mobType, new HashMap<>());
		}
	}
	
	/**
	 * Register a mob bauble slot with a equippingCondition for the mob instance.
	 */
	static <T extends Mob> void registerConditionalSlot(EntityType<? extends T> mobType, Class<T> mobClass,
														String slotKey, Predicate<Mob> condition,
														Function<Mob, ItemStack> accessor)
	{
		updateTypeClassMapping(mobType, mobClass);
		if (!BAUBLE_EQUIPPABLE_MOBS.containsKey(mobType))
			BAUBLE_EQUIPPABLE_MOBS.put(mobType, new HashMap<>());
		if (BAUBLE_EQUIPPABLE_MOBS.get(mobType).containsKey(slotKey))
		{
			LogUtils.getLogger().error("Duplicated bauble slot \"" + slotKey
					+ "\" for EntityType " + mobType.getDescriptionId() + ". Skipped.");
			return;
		}
		BAUBLE_EQUIPPABLE_MOBS.get(mobType).put(slotKey, new Tuple4<>(condition, accessor, false, null));
	}
	
	/**
	 * Register a mob bauble slot without equippingCondition for the mob instance i.e. for all instances of this mob class.
	 */
	static <T extends Mob> void registerGeneralSlot(EntityType<? extends T> mobType, Class<T> mobClass,
													String slotKey, Function<Mob, ItemStack> accessor)
	{
		registerConditionalSlot(mobType, mobClass, slotKey, mob -> true, accessor);
	}
	
	/**
	 * Register a mob special bauble slot with a equippingCondition for the mob instance.
	 * (Special slot: a slot that's unable to equip anything unless Bauble registry entries are manually registered)
	 */
	static <T extends Mob> void registerConditionalSpecialSlot(EntityType<? extends T> mobType, Class<T> mobClass,
											   String slotKey, Predicate<Mob> condition, Function<Mob, ItemStack> accessor)
	{
		updateTypeClassMapping(mobType, mobClass);
		if (!BAUBLE_EQUIPPABLE_MOBS.containsKey(mobType))
			BAUBLE_EQUIPPABLE_MOBS.put(mobType, new HashMap<>());
		if (BAUBLE_EQUIPPABLE_MOBS.get(mobType).containsKey(slotKey))
		{
			LogUtils.getLogger().error("Duplicated bauble slot \"" + slotKey
					+ "\" for EntityType " + mobType.getDescriptionId() + ". Skipped.");
			return;
		}
		BAUBLE_EQUIPPABLE_MOBS.get(mobType).put(slotKey, new Tuple4<>(condition, accessor, true, new HashSet<>()));
	}
	
	/**
	 * Register a mob special bauble slot without equippingCondition for the mob instance i.e. for all instances of this mob class.
	 * (Special slot: a slot that's unable to equip anything unless Bauble registry entries are manually registered) 
	 */
	static <T extends Mob> void registerGeneralSpecialSlot(EntityType<? extends T> mobType, Class<T> mobClass,
										   String slotKey, Function<Mob, ItemStack> accessor)
	{
		registerConditionalSpecialSlot(mobType, mobClass, slotKey, mob -> true, accessor);
	}
	
	// ================= General Utilities =====================
	
	/**
	 * Check if the registry contains a mob type i.e. this mob type is bauble-equippable.
	 */
	static boolean containsMobType(EntityType<?> type)
	{
		return BAUBLE_EQUIPPABLE_MOBS.containsKey(type);
	}
	
	/**
	 * Get all slot keys for a mob.
	 */
	static Set<String> getAllSlotKeys(Mob mob)
	{
		if (!BAUBLE_EQUIPPABLE_MOBS.containsKey(mob.getType()))
			throw new IllegalArgumentException("Mob class " + mob.getClass().getSimpleName() + " is not bauble-equippable.");
		Set<String> keys = new HashSet<>();
		for (String key: BAUBLE_EQUIPPABLE_MOBS.get(mob.getType()).keySet())
		{
			if (BAUBLE_EQUIPPABLE_MOBS.get(mob.getType()).get(key).a.test(mob))
				keys.add(key);
		}
		return keys;
	}
	
	/**
	 * Construct conditions and accessors for all slot keys for a mob.
	 * <p>Call this only on {@code CBaubleEquippableMob} initialization.
	 */
	static Map<String, Function<Mob, ItemStack>> getAllAccessors(Mob mob)
	{
		if (!BAUBLE_EQUIPPABLE_MOBS.containsKey(mob.getType()))
			throw new IllegalArgumentException("Mob class " + mob.getClass().getSimpleName() + " is not bauble-equippable.");
		Map<String, Function<Mob, ItemStack>> map = new HashMap<>();
		for (String key: BAUBLE_EQUIPPABLE_MOBS.get(mob.getType()).keySet())
		{
			if (BAUBLE_EQUIPPABLE_MOBS.get(mob.getType()).get(key).a.test(mob))
				map.put(key, BAUBLE_EQUIPPABLE_MOBS.get(mob.getType()).get(key).b);
		}
		return map;
	}
	
	/**
	 * Get the slot key-property map of a given mob.
	 */
	static Map<String, Tuple4<Predicate<Mob>, Function<Mob, ItemStack>, Boolean, Set<IBaubleRegistryEntry>>> getMobSlotPropertyMap(EntityType<? extends Mob> type)
	{
		if (!containsMobType(type))
			throw new IllegalArgumentException("Mob class \"" + type.getDescriptionId() + "\" is not bauble-equippable, or not registered yet.");
		return BAUBLE_EQUIPPABLE_MOBS.get(type);
	}
	
	
	// ===================== Special Slots related ===================
	
	/**
	 * Check if a slot key of a mob is a special slot. 
	 * Note: if the slot doesn't exist, return false.
	 */
	static boolean isSlotSpecial(EntityType<? extends Mob> type, String key)
	{
		return BAUBLE_EQUIPPABLE_MOBS.containsKey(type) && BAUBLE_EQUIPPABLE_MOBS.get(type).containsKey(key)
				&& BAUBLE_EQUIPPABLE_MOBS.get(type).get(key).c;
	}
	
	/**
	 * Check if a slot key of a mob is a special slot. 
	 * Note: if the slot doesn't exist, return false.
	 */
	@SuppressWarnings("unchecked")
	static boolean isSlotSpecial(Mob mob, String key)
	{
		return isSlotSpecial((EntityType<? extends Mob>)(mob.getType()), key);
	}
	
	/**
	 * Get all registries entries that MAY be accepted by a special slot. (They will still need to pass the checks inside IBaubleRegistryEntry to be equippable)
	 * <p>Null if it's not a special slot.
	 */
	@Nullable
	static Set<IBaubleRegistryEntry> getSpecialSlotAcceptedEntries(Mob mob, String key)
	{
		if (isSlotSpecial(mob, key))
			return BAUBLE_EQUIPPABLE_MOBS.get(mob.getType()).get(key).d;
		else return null;
	}

	/**
	 * Check if a special slot can accept a given itemstack.
	 * <p>Note: if the slot doesn't exist, return false. If the slot exists but isn't special, throws exception.
	 */
	static boolean canSpecialSlotAccept(Mob mob, String key, ItemStack test)
	{
		var set = getSpecialSlotAcceptedEntries(mob, key);
		if (set != null)
		{
			for (IBaubleRegistryEntry entry: set)
			{
				if (entry.getEquippingCondition().test(new BaubleProcessingArgs(test, NFUBaubleAPI.getCapability(mob), key)))
					return true;
			}
			return false;
		}
		else if (BAUBLE_EQUIPPABLE_MOBS.containsKey(mob.getType()) && BAUBLE_EQUIPPABLE_MOBS.get(mob.getType()).containsKey(key))
			throw new IllegalArgumentException("Bauble slot \"" + key + "\" for mob class " + mob.getClass().getSimpleName() + "\" isn't special." );
		else return false;
	}
	
	/**
	 * Add a bauble entry for a special slot to accept.
	 */
	static void addSpecialSlotAccepted(EntityType<? extends Mob> type, String slot, ResourceLocation entryKey)
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
	static void addSpecialSlotAccepted(EntityType<? extends Mob> type, String slot, String entryKey)
	{
		addSpecialSlotAccepted(type, slot, new ResourceLocation(entryKey));
	}

	@Nullable
	static Class<? extends Mob> getEquippableMobClassByType(EntityType<? extends Mob> type) {
		return MOB_TYPE_TO_CLASS_MAPPING.get(type);
	}

	static <T extends Mob> void updateTypeClassMapping(EntityType<? extends T> type, Class<T> clazz) {
		if (MOB_TYPE_TO_CLASS_MAPPING.get(type) == null) {
			MOB_TYPE_TO_CLASS_MAPPING.put(type, clazz);
		} else {
			if (MOB_TYPE_TO_CLASS_MAPPING.get(type).isAssignableFrom(clazz))
				MOB_TYPE_TO_CLASS_MAPPING.put(type, clazz);
		}
	}

}
