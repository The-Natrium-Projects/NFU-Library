package net.sodiumzh.nfu.item.bauble;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.registry.NFURegistry;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * NFFServices - BaubleSystem is a system for equipping "bauble" items on <b>mobs</b> just like for players in Curios API.
 * <p>This class involves some common static methods.
 */
public class NFUBaubleAPI
{
	public static void init() {}

	public static final NFURegistry<BaubleEquippingCondition> EQUIPPING_CONDITIONS
		= new NFURegistry<>(new ResourceLocation(NFULibrary.MOD_ID, "bauble_equipping_conditions"));

	/**
	 * Get the Bauble-handling capability of a mob.
	 * @return Bauble-handling capability ({@link CBaubleEquippableMob}).
	 * If the mob isn't bauble-equippable (not registered or is pending removal, etc.), return an empty instance that won't do anything. 
	 */
	@Nonnull
	static CBaubleEquippableMob getCapability(Mob mob)
	{
		return CBaubleEquippableMob.getCapability(mob);
	}

	/**
	 * Get the Bauble-handling capability of a mob.
	 * @return Bauble-handling capability ({@link CBaubleEquippableMob}).
	 * If the mob isn't bauble-equippable (not registered or is pending removal, etc.), return an empty instance that won't do anything.
	 */
	@Nonnull
	static Optional<CBaubleEquippableMob> getOptionalCapability(Mob mob)
	{
		return CBaubleEquippableMob.getOptionalCapability(mob);
	}

	/**
	 * Check if a mob has the Bauble-handling capability ({@link CBaubleEquippableMob}).
	 */
	public static boolean isCapabilityPresent(Mob mob)
	{
		return getOptionalCapability(mob).isPresent();
	}
	
	/**
	 * Do something if a mob has the Bauble-handling capability ({@link CBaubleEquippableMob}).
	 */
	public static void ifCapabilityPresent(Mob mob, Consumer<Mob> action)
	{
		getOptionalCapability(mob).ifPresent(m -> action.accept(m.getMob()));
	}
	
	/**
	 * Get the current ItemStack equipped on the given slot key <b>as a copy</b>. Empty if the slot isn't present.
	 */
	@Nonnull
	public static ItemStack getSlotItem(Mob mob, String slotKey)
	{
		CBaubleEquippableMob cap = getCapability(mob);
		return cap.getBaubleSlotAccessor().hasSlot(slotKey) ? cap.getBaubleSlotAccessor().getItemStack(slotKey) : ItemStack.EMPTY;
	}
	
	/**
	 * Get all slot keys and equipping items.
	 */
	@Nonnull
	public static Map<String, ItemStack> getAllSlotItems(Mob mob)
	{
		//Map<String, ItemStack> map = new HashMap<>();
		return getOptionalCapability(mob).map(c -> c.getBaubleSlotAccessor().getItemStacks()).orElseGet(HashMap::new);
	}
	
	/**
	 * Do an operation to all bauble registry entries that should take effect to a given slot of a given mob.
	 */
	public static void forEachMatchedEntry(Mob mob, String slot, Consumer<IBaubleRegistryEntry> operationForEach)
	{
		getOptionalCapability(mob).ifPresent(m -> BaubleRegistries.forEachMatchedEntry(m, slot, operationForEach));
	}
	
	/**
	 * Get all entries which the input ItemStack is in the definitions, despite the mob and the slot.
	 */
	public static Set<IBaubleRegistryEntry> getAllRelatedEntries(ItemStack itemstack)
	{
		return BaubleRegistries.getAllRelatedEntries(itemstack);
	}
	
	/**
	 * Check if an ItemStack can be equipped on a given mob into a given slot.
	 */
	public static boolean canEquipOn(ItemStack itemstack, Mob mob, String slot)
	{
		return BaubleRegistries.canEquipOn(itemstack, mob, slot);
	}

	/**
	 * Find a mob's Java class from type if it's bauble-equippable. Empty if not bauble-equippable.
	 */
	public static Optional<Class<? extends Mob>> getBaubleEquippableMobType(EntityType<? extends Mob> mobType) {
		return Optional.ofNullable(BaubleEquippableMobRegistries.getEquippableMobClassByType(mobType));
	}

	/**
	 * Get all single-item baubles (i.e. bauble records defined for specific items).
	 * <p>Note: costly, don't call on tick. To access on tick, use {@link NFUBaubleAPI#getItemKeyedSingleItemBaubles}.
	 */
	public static Map<ResourceLocation, BaubleRecord.SingleItem> getSingleItemBaubles() {
		Map<ResourceLocation, BaubleRecord.SingleItem> res = new HashMap<>();
		BaubleRegistries.SINGLE_REGISTRY.forEach((item, entryList) -> {
			entryList.forEach(entry -> res.put(entry.key(), new BaubleRecord.SingleItem(item, entry.priority(), entry.bauble(), entry.equippingCondition())));
		});
		return Collections.unmodifiableMap(res);
	}

	/**
	 * Get a mapping for single item baubles from each item to its associated bauble entries. This operation is
	 * faster than {@link NFUBaubleAPI#getSingleItemBaubles} and is preferred for calling on tick.
	 */
	public static Map<Item, List<BaubleRegistries.SingleItemBaubleEntry>> getItemKeyedSingleItemBaubles() {
		return Collections.unmodifiableMap(BaubleRegistries.SINGLE_REGISTRY);
	}

	/**
	 * Get all multi-item baubles (i.e. bauble records for multiple items defied by predicates).
	 */
	public static Map<ResourceLocation, BaubleRecord.MultiItem> getMultiItemBaubles() {
		return Collections.unmodifiableMap(BaubleRegistries.MULTIPLE_REGISTRY);
	}

}
