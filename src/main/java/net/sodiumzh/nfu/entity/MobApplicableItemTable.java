package net.sodiumzh.nfu.entity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.function.RegistrableFunction;
import net.sodiumzh.nfu.function.RegistrablePredicate;
import net.sodiumzh.nfu.math.RandomSelection;
import net.sodiumzh.nfu.math.RangedRandomDouble;
import net.sodiumzh.nfu.math.RangedRandomInt;
import net.sodiumzh.nfu.object.Validatable;
import net.sodiumzh.nfu.registry.NFURegistries;

/**
 * A {@code MobApplicableItemTable} is a collection of information about if an {@link ItemStack}
 * is usable to a {@link Mob} and its usage result, including a double "amount" value for e.g. healing,
 * a cool-down time, whether the item should be consumed, and extra actions of the mob.
 */
public class MobApplicableItemTable
{

	public static final MobApplicableItemTable EMPTY = new MobApplicableItemTable();
	
	private HashMap<Input, OutputGetter> entries = new HashMap<>();
	
	protected MobApplicableItemTable()
	{
	}
	
	protected MobApplicableItemTable(HashMap<Input, OutputGetter> entries)
	{
		this.entries = entries;
	}

	/**
	 * Create a new builder.
	 * <p> Note: for a new builder, always call any of {@code add()} first, otherwise it will crash.
	 */
	public static MobApplicableItemTable.Builder builder() {
		return new MobApplicableItemTable.Builder();
	}
	
	/** @deprecated use {@code builder()} instead */
	@Deprecated
	public static MobApplicableItemTable.Builder create() {
		return new MobApplicableItemTable.Builder();
	}
	
	public boolean isEmpty()
	{
		return entries.isEmpty();
	}

	/**
	 *
	 * @param mob
	 * @param stack
	 * @return
	 */
	@Nullable
	public Output getOutput(Mob mob, ItemStack stack)
	{
		for (var input: entries.keySet())
		{
			if (input.test(stack))
				return Output.getOutput(mob, entries.get(input));
		}
		return null;
	}

	@Nullable
	public OutputGetter getOutputGetter(Mob mob, ItemStack stack)
	{
		for (var input: entries.keySet())
		{
			if (input.test(stack))
				return entries.get(input);
		}
		return null;
	}

	@Override
	public String toString()
	{
		return entries.toString();
	}
	
	/**
	 * Only for debug mode, transform it to a visualized map.
	 */
	public HashMap<String, Double> toDebugMap(Mob mob)
	{
		HashMap<String, Double> map = new HashMap<>();
		int i = 0;
		for (var input: entries.keySet())
		{
			if (input.getCriteria(). != null)
				map.put(input.item.toString(), Output.getOutput(mob, entries.get(input)).amount);
			else if (input.tag != null)
				map.put(input.tag.location().toString(), Output.getOutput(mob, entries.get(input)).amount);
			else if (input.stackCheck != null)
			{
				map.put("{Predicate_" + Integer.toString(i) + "}", Output.getOutput(mob, entries.get(input)).amount);
				++i;
			}
			else if (input.key != null)
			{
				Item item = ForgeRegistries.ITEMS.getValue(input.key);
				if (item != null)
					map.put(item.toString(), Output.getOutput(mob, entries.get(input)).amount);
				else map.put("{Missing item: " + input.key.toString() + "}", Output.getOutput(mob, entries.get(input)).amount);
			}
		}
		return map;
	}

	public Map<Input, OutputGetter> getEntriesView() {
		return Map.copyOf(this.entries);
	}

	public static class Builder
	{
		private HashMap<Input, OutputGetter> entries = new HashMap<>();
		private Input buildingActiveEntry = null;
		private DataReader reader = null;

		public MobApplicableItemTable.Builder addRaw(Input in, OutputGetter out)
		{
			entries.put(in, out);
			buildingActiveEntry = in;
			return this;
		}
		/**
		 * Put a new entry.
		 * @param input Raw input object.
		 * @param amount Result amount (fixed value).
		 */
		public MobApplicableItemTable.Builder add(Input input, double amount)
		{
			return addRaw(input, new OutputGetter(amount));
		}
		
		/**
		 * Put a new entry.
		 * @param item Input item.
		 * @param amount Result amount (fixed value).
		 */
		public MobApplicableItemTable.Builder add(Item item, double amount)
		{
			return addRaw(Input.create(item), new OutputGetter(amount));
		}
		
		public MobApplicableItemTable.Builder add(Predicate<ItemStack> predicate, double amount)
		{
			return addRaw(Input.create(predicate), new OutputGetter(amount));
		}
		
		public MobApplicableItemTable.Builder add(TagKey<Item> tag, double amount)
		{
			return addRaw(Input.create(tag), new OutputGetter(amount));
		}
		
		public MobApplicableItemTable.Builder add(ResourceLocation key, double amount)
		{
			return addRaw(Input.create(key), new OutputGetter(amount));
		}

		public MobApplicableItemTable.Builder add(String key, double amount)
		{
			return addRaw(Input.create(key), new OutputGetter(amount));
		}
		
		public MobApplicableItemTable.Builder add(Item item, DoubleValueProvider amount)
		{
			return addRaw(Input.create(item), new OutputGetter(amount));
		}
		
		public MobApplicableItemTable.Builder add(Predicate<ItemStack> predicate, DoubleValueProvider amount)
		{
			return addRaw(Input.create(predicate), new OutputGetter(amount));
		}
		
		public MobApplicableItemTable.Builder add(TagKey<Item> tag, DoubleValueProvider amount)
		{
			return addRaw(Input.create(tag), new OutputGetter(amount));
		}
		
		public MobApplicableItemTable.Builder add(ResourceLocation key, DoubleValueProvider amount)
		{
			return addRaw(Input.create(key), new OutputGetter(amount));
		}
		
		public MobApplicableItemTable.Builder add(String key, DoubleValueProvider amount)
		{
			return addRaw(Input.create(key), new OutputGetter(amount));
		}

		public MobApplicableItemTable.Builder addPredicate(@Nonnull Predicate<ItemStack> predicate)
		{
			if (buildingActiveEntry == null)
				throw new UnsupportedOperationException("Illegal operation for empty table. Call add() first!");
			buildingActiveEntry.addPredicate(predicate);
			return this;
		}

		public MobApplicableItemTable.Builder cooldown(int value)
		{
			if (buildingActiveEntry == null)
				throw new UnsupportedOperationException("Illegal operation for empty table. Call add() first!");
			entries.get(buildingActiveEntry).cooldown(value);
			return this;
		}
		
		public MobApplicableItemTable.Builder cooldown(@Nonnull IntValueProvider getter)
		{
			if (buildingActiveEntry == null)
				throw new UnsupportedOperationException("Illegal operation for empty table. Call add() first!");
			entries.get(buildingActiveEntry).cooldown(getter);
			return this;
		}
		
		public MobApplicableItemTable.Builder noConsume()
		{
			if (buildingActiveEntry == null)
				throw new UnsupportedOperationException("Illegal operation for empty table. Call add() first!");
			entries.get(buildingActiveEntry).noConsume();
			return this;
		}
		
		public MobApplicableItemTable.Builder extraAction(Consumer<Mob> action)
		{
			if (buildingActiveEntry == null)
				throw new UnsupportedOperationException("Illegal operation for empty table. Call add() first!");
			entries.get(buildingActiveEntry).extraAction(action);
			return this;
		}

		/**
		 * Define a resource location and a method (parser) to read data and merge into the table on building.
		 * @Param loc
		 */
		public MobApplicableItemTable.Builder readData(@Nonnull ResourceLocation loc,
													   @Nonnull BiConsumer<JsonElement, MobApplicableItemTable.Builder> parser)
		{
			this.reader = new DataReader(this, loc, parser);
			return this;
		}

		public MobApplicableItemTable build()
		{
			if (this.reader != null) reader.read();
			MinecraftForge.EVENT_BUS.post(new MobApplicableItemTable.BuildEvent(this));
			return new MobApplicableItemTable(this.entries);
		}
	}
	
	/**
	 * A {@link MobApplicableItemTable.Input} is a <i>check</i> for given {@link ItemStack}.
	 * If the input {@link ItemStack} satisfies a specific {@code Input}, the {@code ItemApplyingToMobTable} will return corresponding {@code OutputGetter}.
	 * <p> It now accepts 4 types of checks: 
	 * <p> a) {@link Item}, to check if the given {@link ItemStack} is this type of {@link Item}.
	 * <p> b) {@link ResourceLocation} of item, browsing {@link Item} from registry and check if the {@link ItemStack} is the found {@link Item}.
	 * It could be optional and can be applied for other mods' items. If the item is not found (e.g. due to the mod not loaded), it will be ignored and won't throw exceptions.
	 * <p> c) {@link TagKey} to check if the given {@link ItemStack} has this tag.
	 * <p> d) {@link Predicate<ItemStack>} to simply check if the given {@link ItemStack} meets the condition.
	 * <p> Note: This object is static and should be embedded in the corresponding {@link MobApplicableItemTable}. When
	 * the item interaction actually happens, the item will be checked by each {@link MobApplicableItemTable.Input} to see
	 * which {@link MobApplicableItemTable.OutputGetter}(s) it should use.
	 */
	public static class Input implements Predicate<ItemStack>
	{
		private final ItemStackCriteria criteria;

		private Input(Item item, Predicate<ItemStack> stackCheck, TagKey<Item> tag, ResourceLocation key)
		{
			if (item != null)
				this.criteria = ItemStackCriteria.byItem(item);
			else if (key != null)
				this.criteria = ItemStackCriteria.byItemRegistrykey(key);
			else if (tag != null)
				this.criteria = ItemStackCriteria.byTag(tag);
			else if (stackCheck != null)
				this.criteria = ItemStackCriteria.unparseable(stackCheck);
			else this.criteria = ItemStackCriteria.unparseable(i -> false);
		}

		public ItemStackCriteria getCriteria() {
			return criteria;
		}

		/** Create a raw {@code Input}. Not recommended unless you know what you're doing. */
		public static Input createRaw(Item item, Predicate<ItemStack> stackCheck, TagKey<Item> tag, ResourceLocation key)
		{
			return new Input(item, stackCheck, tag, key);
		}
		
		/** Create from {@link Item}. */
		public static Input create(Item in)
		{
			return new Input(in, null, null, null);
		}
		
		/** Create from {@link Predicate}. */
		public static Input create(Predicate<ItemStack> in)
		{
			return new Input(null, in, null, null);
		}
		
		/** Create from {@link TagKey}. */
		public static Input create(TagKey<Item> in)
		{
			return new Input(null, null, in, null);
		}
		
		/** Create from {@link ResourceLocation} as registry key. */
		public static Input create(ResourceLocation in)
		{
			return new Input(null, null, null, in);
		}
		
		/** Create from {@link ResourceLocation} as registry key. The input string should be formatted like "modid:name_key" to create a {@link ResourceLocation} in-situ. */
		public static Input create(String in)
		{
			return new Input(null, null, null, new ResourceLocation(in));
		}

		public void addRegisteredPredicate(ResourceLocation key) {

		}

		public void addUnparseablePredicate(@Nonnull Predicate<ItemStack> predicate)
		{
			if (this.stackCheck == null)
				this.stackCheck = predicate;
			else this.stackCheck = this.stackCheck.and(predicate);
		}
		
		@Override
		public boolean test(ItemStack stack)
		{
			boolean retval = true;
			boolean valid = false;
			if (item != null)
			{
				retval = retval && stack.is(item);
				valid = true;
				if (!retval) return false;
			}
			if (stackCheck != null)
			{
				try {
					retval = retval && stackCheck.test(stack);
					valid = true;
					if (!retval) return false;
				} catch (RuntimeException | NoSuchFieldError | NoSuchMethodError e)
				{
					e.printStackTrace();
					return false;
				}
			}
			if (tag != null)
			{
				retval = retval && stack.is(tag);
				valid = true;
				if (!retval) return false;
			}
			if (key != null)
			{
				Item item = ForgeRegistries.ITEMS.getValue(key);
				retval = retval && item != null && stack.is(item);
				valid = true;
				if (!retval) return false;
			}
			return retval && valid;
		}
		
		@Override
		public String toString()
		{
			String out = "Input {";
			if (item != null)
				out = out + "Item (" + ForgeRegistries.ITEMS.getKey(item).toString() + "), ";
			if (stackCheck != null)
				out = out + "{Predicate}, ";
			if (tag != null)
				out = out + "Tag (" + tag.location().toString() + "), ";
			if (key != null)
				out = out + "Key (" + key.toString() + "), ";
			if (out.substring(out.length() - 2, out.length()) == ", ")
				out = out.substring(0, out.length() - 2);
			out = out + "}";
			return out;
		}

		/**
		 * Get a list of all items that should be applicable in this input.
		 * <p>Note: when this input is using a predicate, this method can <i>NOT</i>
		 * recognize items of which the default instance is not applicable but only
		 * applicable when having some kind of NBT.
		 * @param refreshCache When this input is using a predicate, if true, it should iterate
		 *                     the whole item registry each time, which may be resource-costly. Use this only
		 *                     when you suspect the item registry itself may have changed after first calling
		 *                     this method.
		 */
		public List<Item> getAllItems(boolean refreshCache) {
			try {
				if (item != null) return List.of(item);
				else if (key != null)
					return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(key)).map(List::of).orElseGet(List::of);
				else if (tag != null)
					return Optional.ofNullable(ForgeRegistries.ITEMS.tags())
						.map(t -> t.getTag(tag).stream().toList()).orElseGet(List::of);
				else {
					// Skip and don't cache if the item registry is not yet available,
					// so it will always retry for the next time
					if (ForgeRegistries.ITEMS.getValues().isEmpty()) return List.of();
					if (this.cachedApplicableItems == null || refreshCache)
						this.cachedApplicableItems = ForgeRegistries.ITEMS.getValues()
							.stream().filter(item -> this.test(item.getDefaultInstance()))
							.toList();
					return this.cachedApplicableItems;
				}
			} catch (RuntimeException e) {
				return List.of();
			}
		}

		/**
		 * Get a list of all items that should be applicable in this input.
		 * <p>Note: when this input is using a predicate, this method can <i>NOT</i>
		 * recognize items of which the default instance is not applicable but only
		 * applicable when having some kind of NBT.
		 * <p>Note: When this input is using a predicate, this operation will need
		 * to iterate the whole Forge item registry, and it will cache the result on
		 * the first run of {@code getAllItems()} by default to save resource. If you
		 * suspect the Forge item registry itself changed after the first run, use
		 * {@code getAllItems(true)} to refresh the cache.
		 */
		public List<Item> getAllItems() {
			return this.getAllItems(false);
		}
	}

	/**
	 * A {@link MobApplicableItemTable.OutputGetter} describes what should happen if
	 * a specific {@link MobApplicableItemTable.Input} is applied to the mob, including a double "amount" value (can represent
	 * anything you want), usage cooldown ticks, whether the item should be consumed, and extra action to take.
	 * <p>This object should be static and embedded in the corresponding {@link MobApplicableItemTable}.
	 */
	public static class OutputGetter
	{
		protected DoubleValueProvider amountProvider = DoubleValueProvider.singleNumber(5d);
		protected IntValueProvider cooldownProvider = IntValueProvider.singleNumber(40);
		protected boolean noConsume = false;
		protected Consumer<Mob> extraAction = mob -> {};
		
		public OutputGetter(double amount) {
			amountProvider = DoubleValueProvider.singleNumber(amount);
		}
		
		public OutputGetter(DoubleValueProvider provider) {
			this.amountProvider = provider;
		}
		
		public void cooldown(int value)
		{
			cooldownProvider = IntValueProvider.singleNumber(value);
		}
		
		public void cooldown(IntValueProvider provider)
		{
			cooldownProvider = provider;
		}
		
		public void noConsume()
		{
			noConsume = true;
		}
		
		public void extraAction(@Nullable Consumer<Mob> action)
		{
			this.extraAction = action;
		}

		public boolean isNoConsume() { return noConsume; }

		public DoubleValueProvider getAmountProvider() {
			return amountProvider;
		}

		public IntValueProvider getCooldownProvider() {
			return cooldownProvider;
		}
	}
	
	/**
	 * A {@link MobApplicableItemTable.Output} represents a result when the item is <i>actually</i> applied to the mob.
	 *
	 */
	public static record Output(Double amount, int cooldown, boolean noConsume, Consumer<Mob> action)
	{
		
		public static Output getOutput(Mob mob, OutputGetter getter)
		{
			return new Output(
				getter.getAmountProvider().apply(mob),
				getter.getCooldownProvider().apply(mob),
				getter.noConsume,
				getter.extraAction);
		}
	}
	
	public static class BuildEvent extends Event
	{
		public final MobApplicableItemTable.Builder builder;
		
		public BuildEvent(MobApplicableItemTable.Builder builder)
		{
			this.builder = builder;
		}
	}

	private static class DataReader
	{
		private ResourceLocation location;
		private MobApplicableItemTable.Builder builder;
		private BiConsumer<JsonElement, Builder> parser;

		public DataReader(MobApplicableItemTable.Builder builder, ResourceLocation location, BiConsumer<JsonElement, Builder> parser)
		{
			this.location = location;
			this.builder = builder;
			this.parser = parser;
		}

		public void read()
		{
			if (builder == null || location == null) return;
			MinecraftServer server = NFULibrary.getServer();
			if (server == null) return;
			ResourceManager mgr = server.getResourceManager();
			List<Resource> resources = mgr.getResourceStack(location);
			for (Resource r: resources)
			{
				try {
					InputStream input = r.open();
					Reader reader = new InputStreamReader(input);
					JsonElement json = JsonParser.parseReader(reader);
					parser.accept(json, builder);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static class ItemStackCriteria implements Predicate<ItemStack> {

		private final Item item;
		private final TagKey<Item> tag;
		private final ResourceLocation itemRegistryKey;
		private Tuple2<ResourceLocation, RegistrablePredicate<ItemStack>> registeredPredicate;
		private Predicate<ItemStack> unparseablePredicate;
		private List<Item> allItemsCache = null;

		private ItemStackCriteria(
			Item item,
			TagKey<Item> tag,
			ResourceLocation itemRegistryKey,
			ResourceLocation predicateKey,
			Predicate<ItemStack> unparseable) {
			this.item = item;
			this.tag = tag;
			this.itemRegistryKey = itemRegistryKey;
			this.unparseablePredicate = unparseable;
		}

		public boolean isValid() {
			return !Stream.of(this.item != null, this.tag != null, this.itemRegistryKey != null, this.registeredPredicate != null, this.unparseablePredicate != null)
                .filter(Boolean::booleanValue).toList().isEmpty();
		}

		public static ItemStackCriteria byItem(Item item) {
			return new ItemStackCriteria(item, null, null, null, null);
		}

		public static ItemStackCriteria byTag(TagKey<Item> tag) {
			return new ItemStackCriteria(null, tag, null, null, null);
		}

		public static ItemStackCriteria byTag(ResourceLocation key) {
			return byTag(TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), key));
		}

		public static ItemStackCriteria byItemRegistrykey(ResourceLocation key) {
			return new ItemStackCriteria(null, null, key, null, null);
		}

		public static ItemStackCriteria byRegisteredPredicate(ResourceLocation regKey) {
			return new ItemStackCriteria(null, null, null, regKey, null);
		}

		public static ItemStackCriteria unparseable(Predicate<ItemStack> predicate) {
			return new ItemStackCriteria(null, null, null, null, predicate);
		}

		@Override
		public boolean test(ItemStack itemStack) {
			if (!this.isValid()) return false;
			boolean res = true;
			if (this.item != null) res = itemStack.is(item);
			else if (this.tag != null) res = itemStack.is(tag);
			else if (this.itemRegistryKey != null)
				res = Optional.ofNullable(ForgeRegistries.ITEMS.getValue(this.itemRegistryKey)).filter(itemStack::is).isPresent();
			if (registeredPredicate != null)
				res = res && registeredPredicate.getB().test(itemStack);
			if (unparseablePredicate != null)
				res = res && unparseablePredicate.test(itemStack);
			return res;
		}

		public void setRegisteredPredicate(Tuple2<ResourceLocation, RegistrablePredicate<ItemStack>> registeredPredicate) {
			this.registeredPredicate = registeredPredicate;
		}

		public void setUnparseablePredicate(Predicate<ItemStack> unparseablePredicate) {
			this.unparseablePredicate = unparseablePredicate;
		}

		public List<Item> getAllUsableItems() {
			if (this.item != null) return List.of(this.item);
			else if (this.tag != null) return Optional.ofNullable(ForgeRegistries.ITEMS.tags()).map(tags -> tags.getTag(this.tag).stream().toList()).orElseGet(List::of);
			else if (itemRegistryKey != null) return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(itemRegistryKey)).map(List::of).orElseGet(List::of);
			// If unparseable, iterate the registry only once and cache the result for further queries
			if (allItemsCache != null) return allItemsCache;
			allItemsCache = ForgeRegistries.ITEMS.getValues().stream().filter(i -> {
				try {
					return this.test(i.getDefaultInstance());
				} catch (Exception e) {
					return false;
				}
			}).toList();
			return allItemsCache;
		}

		public Optional<Item> asItem() {
			if (this.item != null)
				return Optional.of(this.item);
			else if (this.itemRegistryKey != null)

		}

		public Optional<>





	}

	/**
	 * Parse-able double provider from a mob. For generating amount values.
	 */
	public static class DoubleValueProvider implements Function<Mob, Double> {

		public static final DoubleValueProvider INVALID = new DoubleValueProvider(Double.NaN, null, null, null, null);
		private final double singleNumber;  // If invalid, use NaN.
		private final RangedRandomDouble range;
		private final RandomSelection<Double> randomSelection;
		private final Tuple2<ResourceLocation, RegistrableFunction<Mob, Double>> registered;
		private final Function<Mob, Double> unparseableFunction;

		private DoubleValueProvider(
			double singleNumber,
			RangedRandomDouble range,
			RandomSelection<Double> randomSelection,
			ResourceLocation functionKey,
			Function<Mob, Double> unparseableFunction)
		{
			this.singleNumber = singleNumber;
			this.range = range;
			this.randomSelection = randomSelection;
			this.registered = Optional.ofNullable(functionKey)
				.flatMap(k -> NFURegistries.FUNCTIONS.getOptionalValue(k)
					.flatMap(f -> f.castTypes(Mob.class, Double.class))
					.map(f -> Tuple2.of(k, f)))
				.orElse(null);
			this.unparseableFunction = unparseableFunction;
		}

		public static DoubleValueProvider getInvalid() {
			return INVALID;
		}

		public boolean isValid() {
			return Stream.of(!Double.isNaN(singleNumber), range != null, randomSelection != null, registered != null, unparseableFunction != null)
				.filter(Boolean::booleanValue).toList().size() == 1;
		}

		public static DoubleValueProvider singleNumber(double value) {
			return new DoubleValueProvider(value, null, null, null, null);
		}

		public static DoubleValueProvider range(RangedRandomDouble range) {
			return new DoubleValueProvider(Double.NaN, range, null, null, null);
		}

		public static DoubleValueProvider randomSelection(RandomSelection<Double> randomSelection) {
			return new DoubleValueProvider(Double.NaN, null, randomSelection, null, null);
		}

		public static DoubleValueProvider functionKey(ResourceLocation resourceLocation) {
			return new DoubleValueProvider(Double.NaN, null, null, resourceLocation, null);
		}

		public static DoubleValueProvider unparseable(Function<Mob, Double> function) {
			return new DoubleValueProvider(Double.NaN, null, null, null, function);
		}

		public Optional<Double> getAsSingleNumber() {
			if (!isValid()) return Optional.empty();
			return Optional.of(singleNumber).filter(v -> !Double.isNaN(v));
		}

		public Optional<RangedRandomDouble> getAsRange() {
			if (!isValid()) return Optional.empty();
			return Optional.ofNullable(range);
		}

		public Optional<RandomSelection<Double>> getAsSelection() {
			if (!isValid()) return Optional.empty();
			return Optional.ofNullable(randomSelection);
		}

		public Optional<Tuple2<ResourceLocation, RegistrableFunction<Mob, Double>>> getAsRegisteredFunction() {
			if (!isValid()) return Optional.empty();
			return Optional.ofNullable(registered);
		}

		public Optional<Function<Mob, Double>> getAsUnparseableFunction(){
			if (!isValid()) return Optional.empty();
			return Optional.ofNullable(unparseableFunction);
		}

		@Override
		public Double apply(Mob mob) {
			if (!this.isValid()) return 0d;
			if (!Double.isNaN(this.singleNumber)) return this.singleNumber;
			if (range != null) return range.getValue(mob.getRandom());
			if (randomSelection != null) return randomSelection.select(mob.getRandom());
			if (registered != null) return registered.getB().apply(mob);
			if (unparseableFunction != null) return unparseableFunction.apply(mob);
			return 0d;
		}
	}

	/**
	 * Parse-able int provider from a mob. For generating cooldown  values.
	 */
	public static class IntValueProvider implements Function<Mob, Integer> {

		public static final IntValueProvider INVALID = new IntValueProvider(null, null, null, null, null);
		private final Optional<Integer> singleNumber;
		private final RangedRandomInt range;
		private final RandomSelection<Integer> randomSelection;
		private final Tuple2<ResourceLocation, RegistrableFunction<Mob, Integer>> registered;
		private final Function<Mob, Integer> unparseableFunction;

		private IntValueProvider(
			Optional<Integer> singleNumber,
			RangedRandomInt range,
			RandomSelection<Integer> randomSelection,
			ResourceLocation functionKey,
			Function<Mob, Integer> unparseableFunction)
		{
			this.singleNumber = singleNumber;
			this.range = range;
			this.randomSelection = randomSelection;
			this.registered = Optional.ofNullable(functionKey)
				.flatMap(k -> NFURegistries.FUNCTIONS.getOptionalValue(k)
					.flatMap(f -> f.castTypes(Mob.class, Integer.class))
					.map(f -> Tuple2.of(k, f)))
				.orElse(null);
			this.unparseableFunction = unparseableFunction;
		}

		public static IntValueProvider getInvalid() {
			return INVALID;
		}

		public boolean isValid() {
			return Stream.of(singleNumber.isPresent(), range != null, randomSelection != null, registered != null, unparseableFunction != null)
				.filter(Boolean::booleanValue).toList().size() == 1;
		}

		public static IntValueProvider singleNumber(int value) {
			return new IntValueProvider(Optional.of(value), null, null, null, null);
		}

		public static IntValueProvider range(RangedRandomInt range) {
			return new IntValueProvider(Optional.empty(), range, null, null, null);
		}

		public static IntValueProvider randomSelection(RandomSelection<Integer> randomSelection) {
			return new IntValueProvider(Optional.empty(), null, randomSelection, null, null);
		}

		public static IntValueProvider functionKey(ResourceLocation resourceLocation) {
			return new IntValueProvider(Optional.empty(), null, null, resourceLocation, null);
		}

		public static IntValueProvider unparseable(Function<Mob, Integer> function) {
			return new IntValueProvider(Optional.empty(), null, null, null, function);
		}

		public Optional<Integer> getAsSingleNumber() {
			if (!isValid()) return Optional.empty();
			return singleNumber;
		}

		public Optional<RangedRandomInt> getAsRange() {
			if (!isValid()) return Optional.empty();
			return Optional.ofNullable(range);
		}

		public Optional<RandomSelection<Integer>> getAsSelection() {
			if (!isValid()) return Optional.empty();
			return Optional.ofNullable(randomSelection);
		}

		public Optional<Tuple2<ResourceLocation, RegistrableFunction<Mob, Integer>>> getAsRegisteredFunction() {
			if (!isValid()) return Optional.empty();
			return Optional.ofNullable(registered);
		}

		public Optional<Function<Mob, Integer>> getAsUnparseableFunction(){
			if (!isValid()) return Optional.empty();
			return Optional.ofNullable(unparseableFunction);
		}

		@Override
		public Integer apply(Mob mob) {
			if (!this.isValid()) return 0;
			if (this.singleNumber.isPresent()) return this.singleNumber.get();
			if (range != null) return range.getValue(mob.getRandom());
			if (randomSelection != null) return randomSelection.select(mob.getRandom());
			if (registered != null) return registered.getB().apply(mob);
			if (unparseableFunction != null) return unparseableFunction.apply(mob);
			return 0;
		}
	}
}
