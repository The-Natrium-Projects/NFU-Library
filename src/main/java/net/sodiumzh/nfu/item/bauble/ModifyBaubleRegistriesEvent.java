package net.sodiumzh.nfu.item.bauble;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.container.Tuple4;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
/**
 * Fired after {@link RegisterBaublesEvent} and bauble registry clear-up, allowing to modify bauble behaviors after registration.
 */
public class ModifyBaubleRegistriesEvent extends Event
{
	/** Get entry from the Single Bauble Registry (i.e. baubles for specific items). Null if not found. */
	@Nullable
	private static Tuple2<Item, BaubleRegistries.SingleItemBaubleEntry> getSingleEntry(ResourceLocation key)
	{
		return BaubleRegistries.SINGLE_REGISTRY.entrySet().stream()
			.flatMap(itemEntry -> itemEntry.getValue().stream().map(entry -> new Tuple2<>(itemEntry.getKey(), entry)))
			.findAny().orElse(null);
	}
	
	/** Get entry from the Multiple Bauble Registry (i.e. baubles defined by predicates). Null if not found. */
	@Nullable
	private static BaubleRecord.MultiItem getMultipleEntry(ResourceLocation key)
	{
		return BaubleRegistries.MULTIPLE_REGISTRY.get(key);
	}
	
	/**
	 * Modify a single-item bauble registry entry's priority by operator (current value dependent).
	 */
	public void modifyPriority(ResourceLocation entryKey, UnaryOperator<Double> operator)
	{
		Tuple2<Item, BaubleRegistries.SingleItemBaubleEntry> entry = getSingleEntry(entryKey);
		if (entry == null)
		{
			LogUtils.getLogger().warn("NFU Bauble System: Single Bauble Entry \"" + entryKey.toString() + "\" not found. Modification is skipped.");
			return;
		}
		BaubleRegistries.SINGLE_REGISTRY.get(entry.getA()).remove(entry.getB());
		BaubleRegistries.SingleItemBaubleEntry newEntry =
			new BaubleRegistries.SingleItemBaubleEntry(operator.apply(entry.getB().priority()), entry.getB().key(), entry.getB().bauble(), entry.getB().equippingCondition());
		BaubleRegistries.SINGLE_REGISTRY.get(entry.getA()).add(newEntry);
	}
	
	/**
	 * Modify a single-item bauble registry entry's priority to a fixed value.
	 */
	public void modifyPriority(ResourceLocation entryKey, double value)
	{
		this.modifyPriority(entryKey, v -> value);
	}
	
	/**
	 * Modify a bauble registry entry's equipping equippingCondition.
	 */
	public void modifyEquippingCondition(ResourceLocation entryKey, Consumer<BaubleEquippingCondition> modification)
	{
		var singleEntry = getSingleEntry(entryKey);
		if (singleEntry != null)
		{
			modification.accept(singleEntry.getB().equippingCondition());
			return;
		}
		var multipleEntry = getMultipleEntry(entryKey);
		if (multipleEntry != null)
		{
			modification.accept(multipleEntry.equippingCondition());
			return;
		}
		LogUtils.getLogger().warn("NFU Bauble System: Bauble Entry \"" + entryKey.toString() + "\" not found. Modification is skipped.");
	}
	
	/**
	 * Extend a new definition to a multiple-item bauble registry entry. An item will satisfy <b>either</b> the old or new conditions to be defined as this bauble.
	 */
	public void extendMultipleItemDefinition(ResourceLocation entryKey, BiPredicate<Item, ItemStack> extendedDefinition)
	{
		var multipleEntry = getMultipleEntry(entryKey);
		if (multipleEntry != null)
		{
			var old = multipleEntry.predicate();
			BaubleRecord.MultiItem newEntry = new BaubleRecord.MultiItem (old.or(extendedDefinition), multipleEntry.bauble(), multipleEntry.equippingCondition());
			BaubleRegistries.MULTIPLE_REGISTRY.put(entryKey, newEntry);
		}
		else
		{
			LogUtils.getLogger().warn("NFU Bauble System: Multiple Bauble Entry \"" + entryKey.toString() + "\" not found. Modification is skipped.");
		}
	}
	
	/**
	 * Shrink the definition of a multiple-item bauble registry entry. An item will satisfy <b>both</b> the old and new conditions to be defined as this bauble.
	 */
	public void shrinkMultipleItemDefinition(ResourceLocation entryKey, BiPredicate<Item, ItemStack> additionalDefinitionCheck)
	{
		var multipleEntry = getMultipleEntry(entryKey);
		if (multipleEntry != null)
		{
			var old = multipleEntry.predicate();
			BaubleRecord.MultiItem newEntry = new BaubleRecord.MultiItem(old.and(additionalDefinitionCheck), multipleEntry.bauble(), multipleEntry.equippingCondition());
			BaubleRegistries.MULTIPLE_REGISTRY.put(entryKey, newEntry);
		}
		else
		{
			LogUtils.getLogger().warn("NFU Bauble System: Multiple Bauble Entry \"" + entryKey.toString() + "\" not found. Modification is skipped.");
		}
	}
}
