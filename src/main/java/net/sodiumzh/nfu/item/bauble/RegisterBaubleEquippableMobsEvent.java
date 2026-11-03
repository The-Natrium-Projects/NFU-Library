package net.sodiumzh.nfu.item.bauble;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class RegisterBaubleEquippableMobsEvent extends Event implements IModBusEvent
{

	/**
	 * Register a mob class as bauble-equippable. It returns a {@code SlotRegisterer} for further registering slots.
	 */
	public <T extends Mob> SlotRegisterer<T> register(EntityType<? extends T> type, Class<T> clazz)
	{
		if (!BaubleEquippableMobRegistries.containsMobType(type))
			BaubleEquippableMobRegistries.registerMob(type, clazz);
		return new SlotRegisterer<>(type, clazz);
	}

	public static record SlotRegisterer<T extends Mob>(EntityType<? extends T> type, Class<T> clazz)
	{
		/**
		 * Add a slot for all mobs of this class.
		 */
		public SlotRegisterer<T> addSlot(String key, Function<Mob, ItemStack> accessor)
		{
			BaubleEquippableMobRegistries.registerGeneralSlot(type, clazz, key, accessor);
			return this;
		}
		
		/**
		 * Add a slot only for mobs satisfying the condition in the class.
		 * @param condition Condition for the mob to have this slot.
		 */
		public SlotRegisterer<T> addConditionalSlot(String key, Predicate<Mob> condition, Function<Mob, ItemStack> accessor)
		{
			BaubleEquippableMobRegistries.registerConditionalSlot(type, clazz, key, condition, accessor);
			return this;
		}
		
		/**
		 * Add a special slot for all mobs of this class. A special slot is a slot that doesn't accept any baubles unless manually registered.
		 */
		public SpecialSlotRegisterer<T> addSpecialSlot(String key, Function<Mob, ItemStack> accessor)
		{
			BaubleEquippableMobRegistries.registerGeneralSpecialSlot(type, clazz, key, accessor);
			return new SpecialSlotRegisterer<>(type, clazz, key);
		}
		
		/**
		 * Add a special slot only for mobs satisfying the condition in the class. A special slot is a slot that doesn't accept any baubles unless manually registered.
		 * @param condition Condition for the mob to have this slot.
		 */
		public SpecialSlotRegisterer<T> addConditionalSpecialSlot(String key, Predicate<Mob> condition, Function<Mob, ItemStack> accessor)
		{
			BaubleEquippableMobRegistries.registerConditionalSpecialSlot(type, clazz, key, condition, accessor);
			return new SpecialSlotRegisterer<>(type, clazz, key);
		}		
	}
	
	public static record SpecialSlotRegisterer<T extends Mob>(EntityType<? extends T> type, Class<T> clazz, String slotKey)
	{
		/**
		 * Register accepted Bauble Entries for a special slot. Entry keys uses ResourceLocation format.
		 * @return Go back to slot registerer for further registering slots for the mob class.
		 */
		public SlotRegisterer<T> addSpecialSlotItem(String... acceptedEntryKeys)
		{
			for (String entryKey: List.of(acceptedEntryKeys))
				BaubleEquippableMobRegistries.getMobSlotPropertyMap(type).get(slotKey).d.add(BaubleRegistries.getEntryByKey(entryKey));
			return new SlotRegisterer<T>(type, clazz);
		}
		
		/**
		 * Register accepted Bauble Entries for a special slot.
		 * @return Go back to slot registerer for further registering slots for the mob class.
		 */
		public SlotRegisterer<T> addSpecialSlotItem(ResourceLocation... acceptedEntryKeys)
		{
			for (ResourceLocation entryKey: List.of(acceptedEntryKeys))
				BaubleEquippableMobRegistries.getMobSlotPropertyMap(type).get(slotKey).d.add(BaubleRegistries.getEntryByKey(entryKey));
			return new SlotRegisterer<>(type, clazz);
		}
	}
}
