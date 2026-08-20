package net.sodiumzh.nfu.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.common.MinecraftForge;
import net.neoforged.eventbus.api.Event;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.function.RegistrableFunction;
import net.sodiumzh.nfu.function.RegistrablePredicate;
import net.sodiumzh.nfu.math.RandomSelection;
import net.sodiumzh.nfu.math.RangedRandomDouble;
import net.sodiumzh.nfu.math.RangedRandomInt;
import net.sodiumzh.nfu.registry.NFURegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@code MobApplicableItemTable} is a collection of information about if an {@link ItemStack}
 * is usable to a {@link Mob} and its usage result, including a double "amount" value for e.g. healing,
 * a cool-down time, whether the item should be consumed, and extra actions of the mob.
 */
public class MobApplicableItemTable
{

	public static final MobApplicableItemTable EMPTY = new MobApplicableItemTable();
	
	private HashMap<ItemStackCriteria, OutcomeProvider> entries = new HashMap<>();
	
	protected MobApplicableItemTable()
	{
	}
	
	protected MobApplicableItemTable(HashMap<ItemStackCriteria, OutcomeProvider> entries)
	{
		this.entries = entries;
	}

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

	public Optional<Outcome> getOutcome(Mob mob, ItemStack stack)
	{
		return this.getOutcomeProvider(mob, stack).map(provider -> Outcome.getOutcome(mob, provider));
	}

	public Optional<OutcomeProvider> getOutcomeProvider(Mob mob, ItemStack stack)
	{
        return entries.entrySet().stream().filter(entry -> entry.getKey().test(stack))
            // Overriding order: item > tag > registrable predicate > unparseable predicate
            .min(Comparator.comparingInt(entry -> {
            if (!entry.getKey().getItemMatchingList().isEmpty())
                return 1;
            else if (!entry.getKey().getTagMatchingList().isEmpty())
                return 2;
            else if (entry.getKey().getRegistrablePredicateCriterion().isPresent())
                return 3;
            else return 4;
        })).map(Map.Entry::getValue);
	}

	@Override
	public String toString()
	{
		return entries.toString();
	}


	public Map<ItemStackCriteria, OutcomeProvider> getEntriesView() {
		return Map.copyOf(this.entries);
	}

	public static class Builder
	{
		private HashMap<ItemStackCriteria, OutcomeProvider> entries = new HashMap<>();
		private DataReader reader = null;

        public MobApplicableItemTable.Builder create() {
            return new MobApplicableItemTable.Builder();
        }

		public MobApplicableItemTable.Builder add(ItemStackCriteria in, OutcomeProvider out)
		{
			entries.put(in, out);
			return this;
		}

		public MobApplicableItemTable.Builder addSimpleOutput(ItemStackCriteria input, double amount, int cooldown)
		{
			return add(input, OutcomeProvider.simple(amount, cooldown));
		}

        public MobApplicableItemTable.Builder addSimpleItem(Item item, double amount, int cooldown) {
            return add(ItemStackCriteria.byItems(item), OutcomeProvider.simple(amount, cooldown));
        }

        public MobApplicableItemTable.Builder addSimpleTag(TagKey<Item> tag, double amount, int cooldown) {
            return add(ItemStackCriteria.byTags(tag), OutcomeProvider.simple(amount, cooldown));
        }

        public MobApplicableItemTable.Builder addSimpleTag(ResourceLocation tag, double amount, int cooldown) {
            return add(ItemStackCriteria.byTags(tag), OutcomeProvider.simple(amount, cooldown));
        }

        public MobApplicableItemTable.Builder addItemRanged(Item item, double min, double max, int cooldown) {
            return add(ItemStackCriteria.byItems(item), OutcomeProvider.ranged(min, max, cooldown));
        }

        public MobApplicableItemTable.Builder addSimpleTag(TagKey<Item> tag, double min, double max, int cooldown) {
            return add(ItemStackCriteria.byTags(tag), OutcomeProvider.ranged(min, max, cooldown));
        }

        public MobApplicableItemTable.Builder addSimpleTag(ResourceLocation tag, double min, double max, int cooldown) {
            return add(ItemStackCriteria.byTags(tag), OutcomeProvider.ranged(min, max, cooldown));
        }

		/**
		 * Define a resource location and a method (parser) to read data and merge into the table on building.
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
     * Criteria to match input {@link ItemStack}s. It has an item list and a tag list to match, and a
     * {@link RegistrablePredicate} and a generic (unparseable) {@link Predicate} for additional checks.
     * <p>The item must be included in <i>either the item or the tag list</i>, and passes <i>both predicates</i>
     * (if present). If the item and tag lists are both empty, the item needs to only pass predicates.
     */
    public static class ItemStackCriteria implements Predicate<ItemStack> {

        private final List<Item> items = new ArrayList<>();
        private final List<TagKey<Item>> tags = new ArrayList<>();
        private Tuple2<ResourceLocation, RegistrablePredicate<ItemStack>> registeredPredicate;
        private Predicate<ItemStack> unparseablePredicate;
        private List<Item> allItemsCache = null;

        private ItemStackCriteria(
            @Nullable ResourceLocation predicateKey,
            @Nullable Predicate<ItemStack> unparseable) {
            this.registeredPredicate = Optional.ofNullable(predicateKey)
                .map(k -> Tuple2.of(k,
                    Optional.ofNullable(NFURegistries.PREDICATES.getValue(k)).flatMap(p -> p.castInputType(ItemStack.class))
                        .orElse(null)))
                .filter(pair -> pair.getB() != null)
                .orElse(null);
            this.unparseablePredicate = unparseable;
        }

        public static ItemStackCriteria create() {
            return new ItemStackCriteria(null, null);
        }

        public static ItemStackCriteria byItems(Object... items) {
            return ItemStackCriteria.create().addItems(items);
        }

        public static ItemStackCriteria byTags(Object... tags) {
            return ItemStackCriteria.create().addTags(tags);
        }

        public static ItemStackCriteria byRegistrablePredicate(ResourceLocation key) {
            return ItemStackCriteria.create().setRegistrablePredicate(key);
        }

        public static ItemStackCriteria byUnparseablePredicate(Predicate<ItemStack> predicate) {
            return ItemStackCriteria.create().setUnparseablePredicate(predicate);
        }

        /**
         * Add items to match. The tested item should be one of the listed items to meet the requirement.
         * @param itemsOrRegistryKeys Either {@link Item}s or {@link ItemStack}s for items (NBT will be ignored),
         *        or {@link ResourceLocation}s or Strings for registry keys. Other random elements, {@code null}s and
         *        invalid keys will be ignored.
         * @return this.
         */
        public ItemStackCriteria addItems(Collection<?> itemsOrRegistryKeys) {
            List<Item> items = itemsOrRegistryKeys.stream().map(elem -> {
                try {
                    if (elem instanceof Item item) return item;
                    else if (elem instanceof ResourceLocation key)
                        return BuiltInRegistries.ITEM.getValue(key);
                    else if (elem instanceof ItemStack itemStack)
                        return itemStack.getItem();
                    else if (elem instanceof String str)
                        return BuiltInRegistries.ITEM.getValue(new ResourceLocation(str));
                    else return Items.AIR;
                } catch (Exception e) {
                    return Items.AIR;
                }
            }).filter(Objects::nonNull).filter(item -> !item.equals(Items.AIR)).toList();
            this.items.addAll(items);
            return this;
        }

        /**
         * Add items to match. The tested item should be one of the listed items to meet the requirement.
         * @param itemsOrRegistryKeys Either {@link Item}s or {@link ItemStack}s for items (NBT will be ignored),
         *        or {@link ResourceLocation}s or Strings for registry keys. Other random elements, {@code null}s and
         *        invalid keys will be ignored.
         * @return this.
         */
        public ItemStackCriteria addItems(Object... itemsOrRegistryKeys) {
            return addItems(List.of(items));
        }

        /**
         * Add items to match. The tested item should have one of the listed tags to meet the requirement.
         * @param keys Either {@link TagKey<Item>}s, or {@link ResourceLocation}s / Strings as keys.
         *        Other random elements and {@code null}s keys will be ignored.
         * @return this.
         */
        public ItemStackCriteria addTags(Collection<?> keys) {
            List<TagKey<Item>> tags = keys.stream().map(elem -> {
                try {
                    if (elem instanceof TagKey<?> tagKey && tagKey.registry().equals(Registries.ITEM))
                        return tagKey;
                    else if (elem instanceof ResourceLocation key)
                        return TagKey.create(Registries.ITEM, key);
                    else if (elem instanceof String str)
                        return TagKey.create(Registries.ITEM, new ResourceLocation(str));
                    else return null;
                } catch (Exception e) {
                    return null;
                }
            }).filter(Objects::nonNull).map(key -> (TagKey<Item>)key).toList();
            this.tags.addAll(tags);
            return this;
        }

        public ItemStackCriteria addTags(Object... tags) {
            return addItems(List.of(tags));
        }

        /**
         * Get all items matching either the item list or the tag list.
         */
        public Set<Item> getAllItemsAndTags() {
            Set<Item> res = this.tags.stream().flatMap(tag -> {
                    List<Item> tagItems = new ArrayList<>();
                    BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> tagItems.add(holder.value()));
                    return tagItems.stream();
                })
                .collect(Collectors.toSet());
            res.addAll(this.items);
            return res;
        }

        public ItemStackCriteria setRegistrablePredicate(@Nullable ResourceLocation key) {
            this.registeredPredicate = Optional.ofNullable(key)
                .map(k -> Tuple2.of(k,
                    Optional.ofNullable(NFURegistries.PREDICATES.getValue(k)).flatMap(p -> p.castInputType(ItemStack.class))
                        .orElse(null)))
                .filter(pair -> pair.getB() != null)
                .orElse(null);
            return this;
        }

        public ItemStackCriteria setUnparseablePredicate(@Nullable Predicate<ItemStack> predicate) {
            this.unparseablePredicate = predicate;
            return this;
        }


        @Override
        public boolean test(ItemStack itemStack) {
            // If this criteria instance is totally invalid, alway return false
            if (this.items.isEmpty() && this.tags.isEmpty() && this.registeredPredicate == null && this.unparseablePredicate == null)
                return false;
            // If items and tags are both empty, the input item will only match predicates
            boolean res = this.items.isEmpty() && this.tags.isEmpty();
            // If the input item matches either item list or tag list, set this to true. If
            // lists and tags are both empty, res is already true, and the following two rows
            // will be skipped
            res = res || this.items.contains(itemStack.getItem());
            res = res || this.tags.stream().anyMatch(itemStack::is);
            // The item must meet both criteria defined by predicates if present
            if (registeredPredicate != null)
                res = res && registeredPredicate.getB().test(itemStack);
            if (unparseablePredicate != null)
                res = res && unparseablePredicate.test(itemStack);
            return res;
        }

        public void setRegisteredPredicate(Tuple2<ResourceLocation, RegistrablePredicate<ItemStack>> registeredPredicate) {
            this.registeredPredicate = registeredPredicate;
        }

        public List<Item> getAllUsableItems() {
            if (allItemsCache != null) return allItemsCache;
            if (this.items.isEmpty() && this.tags.isEmpty() && this.registeredPredicate == null && this.unparseablePredicate == null) {
                allItemsCache = List.of();
                return allItemsCache;
            }
            Collection<Item> allItemsToTest = this.items.isEmpty() && this.tags.isEmpty() ?
                BuiltInRegistries.ITEM.stream().toList() : getAllItemsAndTags();
            this.allItemsCache = allItemsToTest.stream().filter(i -> {
                if (registeredPredicate != null && !registeredPredicate.getB().test(i.getDefaultInstance()))
                    return false;
                else if (unparseablePredicate != null && !unparseablePredicate.test(i.getDefaultInstance()))
                    return false;
                return true;
            }).distinct().toList();
            return allItemsCache;
        }

        public List<Item> getItemMatchingList() {
            return this.items.stream().toList();
        }

        public List<TagKey<Item>> getTagMatchingList() {
            return this.tags.stream().toList();
        }

        public Optional<Tuple2<ResourceLocation, RegistrablePredicate<ItemStack>>> getRegistrablePredicateCriterion() {
            return Optional.ofNullable(this.registeredPredicate);
        }

        public Optional<Predicate<ItemStack>> getUnparseablePredicateCriterion() {
            return Optional.ofNullable(this.unparseablePredicate);
        }
    }


    /**
	 * A {@link OutcomeProvider} describes what should happen if
	 * a specific {@link MobApplicableItemTable.ItemStackCriteria} is applied to the mob, including a double result (can represent
	 * anything you want), an integer result, whether the item should be consumed, and extra action to take.
	 * <p>This object should be static and embedded in the corresponding {@link MobApplicableItemTable}.
     * <p>Note: double and int results are <i>totally separate</i>, and cannot generate values for each other.
	 */
	public static class OutcomeProvider
	{
		protected DoubleValueProvider amountProvider;
        protected IntValueProvider cooldownProvider;
        protected boolean noConsume = false;
		protected Consumer<Mob> extraAction = mob -> {};

        public OutcomeProvider(DoubleValueProvider amountProvider, IntValueProvider cooldownProvider) {
            this.amountProvider = amountProvider;
            this.cooldownProvider = cooldownProvider;
        }

        /**
         * Create an empty provider with initial amount and cooldown 0.
         */
        public static OutcomeProvider empty() {
            return simple(0d, 0);
        }

        /**
         * Create with a single (unrandomized) amount and cooldown.
         */
        public static OutcomeProvider simple(double amount, int cooldown) {
            return new OutcomeProvider(DoubleValueProvider.singleNumber(amount), IntValueProvider.singleNumber(cooldown));
        }

        /**
         * Create with a ranged amount result (uniform distribution) and single cooldown.
         */
        public static OutcomeProvider ranged(double min, double max, int cooldown) {
            return new OutcomeProvider(DoubleValueProvider.range(RangedRandomDouble.uniform(min, max)), IntValueProvider.singleNumber(cooldown));
        }


        public OutcomeProvider setAmountProvider(DoubleValueProvider amountProvider) {
            this.amountProvider = amountProvider;
            return this;
        }

        public OutcomeProvider setAmountValue(double amount) {
            return setAmountProvider(DoubleValueProvider.singleNumber(amount));
        }

        public OutcomeProvider setCooldownProvider(IntValueProvider cooldownProvider) {
            this.cooldownProvider = cooldownProvider;
            return this;
        }

        public OutcomeProvider setCooldownValue(int cooldown) {
            return setCooldownProvider(IntValueProvider.singleNumber(cooldown));
        }

        public boolean isNoConsume() {
            return noConsume;
        }

        public OutcomeProvider setNoConsume(boolean noConsume) {
            this.noConsume = noConsume;
            return this;
        }

        public Consumer<Mob> getExtraAction() {
            return extraAction;
        }

        public OutcomeProvider setExtraAction(Consumer<Mob> extraAction) {
            this.extraAction = extraAction;
            return this;
        }

        public DoubleValueProvider getAmountProvider() {
			return amountProvider;
		}

		public IntValueProvider getCooldownProvider() {
            return cooldownProvider;
		}

        /**
         * Provide a complete outcome record.
         */
        public Outcome getOutcome(Mob mob) {
            return Outcome.getOutcome(mob, this);
        }

        /**
         * Only run amount provider and give a result.
         */
        public double getOutcomeAmount(Mob mob) {
            return amountProvider.apply(mob);
        }

        /**
         * Only run cooldown provider and give a result.
         */
        public int getOutcomeCooldown(Mob mob) {
            return cooldownProvider.apply(mob);
        }

	}
	
	/**
	 * An {@link Outcome} represents a result when the item is <i>actually</i> applied to the mob.
	 */
	public static record Outcome(double amount, int cooldown, boolean noConsume, Consumer<Mob> action)
	{
		
		public static Outcome getOutcome(Mob mob, OutcomeProvider provider)
		{
			return new Outcome(
				provider.getOutcomeAmount(mob),
				provider.getOutcomeCooldown(mob),
				provider.noConsume,
				provider.extraAction);
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
