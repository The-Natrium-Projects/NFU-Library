package net.sodiumzh.nfu.entity.vanillatrade;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.registries.ForgeRegistries;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.container.Tuple3;
import net.sodiumzh.nfu.container.Tuple4;
import net.sodiumzh.nfu.math.ThreadSafeRandomSource;
import net.sodiumzh.nfu.object.Validatable;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.registry.NFURegistry;
import net.sodiumzh.nfu.util.NFUDataStatics;
import net.sodiumzh.nfu.util.NFUDebugStatics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link VanillaTradeRegistry} is a mapping from each {@link ResourceLocation} key and each {@link VillagerProfession} to
 * a collection of {@link VanillaTradeListingCollection}s.
 * <p>Mob instances will access the trade entry generators by this registry. Note that this trade registry is not {@link NFURegistry},
 * but is recommended to be registered into "the registry of trade registries"({@link NFURegistries#VANILLA_TRADE_REGISTRIES}.
 * <p>Note: The {@link ResourceLocation}s are arbitrary as an identifier without pre-defined meanings.
 * <p>Tip: Use {@link VanillaTradeRegistry#collect()} to get the mapping to the merged listing sets from all related collections.
 */
public class VanillaTradeRegistry
{
	private static final RandomSource RND = new ThreadSafeRandomSource();
	// Table: {key, profession} -> {collection, weightScale}
	private final Multimap<Tuple2<ResourceLocation, VillagerProfession>, Tuple2<VanillaTradeListingCollection<?>, Double>> table;

	@Nullable
	private ResourceLocation lastKey = null;
	@Nonnull
	private VillagerProfession lastProfession = VillagerProfession.NONE;
	private Validatable<VanillaTradeRegistry.Collected> collectedCache = new Validatable<>();

	public VanillaTradeRegistry() {
		this.table = HashMultimap.create();
		MinecraftForge.EVENT_BUS.post(new VanillaTradeRegistryEvent(this));
		// Event operations may modify these values. Prevent them from impacting further registrations
		this.lastKey = null;
		this.lastProfession = VillagerProfession.NONE;
	}

	public VanillaTradeRegistry put(ResourceLocation key, VillagerProfession profession, VanillaTradeListingCollection<?> collection, double weightScale) {
		Tuple2<ResourceLocation, VillagerProfession> mapKey = new Tuple2<>(key, profession);
		table.get(mapKey).stream().filter(e -> Objects.equals(e.getA(), collection))
			.findFirst()
			.ifPresentOrElse(
				e -> e.setB(e.getB() + weightScale),
				() -> table.put(mapKey, Tuple2.of(collection, weightScale)));
		lastKey = key;
		lastProfession = profession;
		this.collectedCache.invalidate();
		return this;
	}

	public VanillaTradeRegistry put(ResourceLocation key, VanillaTradeListingCollection<?> value, double weightScale) {
		return put(key, VillagerProfession.NONE, value, weightScale);
	}

	public VanillaTradeRegistry put(ResourceLocation key, VillagerProfession profession, VanillaTradeListingCollection<?>... collections) {
		for (VanillaTradeListingCollection<?> value: collections) {
			this.put(key, profession, value, 1d);
		}
		return this;
	}

	public VanillaTradeRegistry put(ResourceLocation key, VanillaTradeListingCollection<?>... values) {
		return put(key, VillagerProfession.NONE, values);
	}

	public VanillaTradeRegistry put(ResourceLocation key, VillagerProfession profession, Iterable<VanillaTradeListingCollection<?>> values) {
		values.forEach(v -> this.put(key, profession, v, 1d));
		return this;
	}

	public VanillaTradeRegistry put(ResourceLocation key, Iterable<VanillaTradeListingCollection<?>> values) {
		return put(key, VillagerProfession.NONE, values);
	}

	public VanillaTradeRegistry putLast(VillagerProfession profession, VanillaTradeListingCollection<?> value, double weightScale) {
		if (lastKey == null) {
			throw new IllegalStateException("VanillaTradeRegistry#putLast: missing last key. Specify a key by calling any " +
				"key-specific version of put() before calling any key-omitted versions.");
		}
		return put(lastKey, profession, value, weightScale);
	}

	public VanillaTradeRegistry putLast(VillagerProfession profession, VanillaTradeListingCollection<?>... values) {
		if (lastKey == null) {
			throw new IllegalStateException("VanillaTradeRegistry#putLast: missing last key. Specify a key by calling any " +
					"key-specific version of put() before calling any key-omitted versions.");
		}
		return put(lastKey, profession, values);
	}

	public VanillaTradeRegistry putLast(VanillaTradeListingCollection<?> value, double weightScale) {
		if (lastKey == null) {
			throw new IllegalStateException("VanillaTradeRegistry#putLast: missing last key. Specify a key by calling any " +
				"key-specific version of put() before calling any key-omitted versions.");
		}
		return put(lastKey, lastProfession, value, weightScale);
	}

	public VanillaTradeRegistry putLast(VanillaTradeListingCollection<?>... values) {
		if (lastKey == null) {
			throw new IllegalStateException("VanillaTradeRegistry#putLast: missing last key. Specify a key by calling any " +
					"key-specific version of put() before calling any key-omitted versions.");
		}
		return put(lastKey, lastProfession, values);
	}

	public Map<VanillaTradeListingCollection<?>, Double> getCollections(ResourceLocation key, VillagerProfession profession) {
		return table.get(new Tuple2<>(key, profession)).stream().collect(Collectors.toMap(Tuple2::getA, Tuple2::getB));
	}

	public Map<VanillaTradeListingCollection<?>, Double> getCollectionsForDefaultProfession(ResourceLocation key) {
		return getCollections(key, VillagerProfession.NONE);
	}

	public Set<Tuple2<ResourceLocation, VillagerProfession>> keySet() {
		return new HashSet<>(table.keySet());
	}

	/**
	 * Collect all elements and provide a 3-dimensional mapping from (key, profession, merchant level) to
	 * united listings from all listing collections.
	 */
	public Collected collect() {
		if (collectedCache.isValidated()) return collectedCache.get();
		Collected res = new Collected();
		//table.keySet()	// {key, profession}
			/*
			.stream().map(k -> Tuple3.of(k, table.get(k)))	// {key, profession, (all collections and weight scales)}
			// For each key-profession pair
			.forEach(entry -> entry.c.stream() // {collection, weight scale}
				// Map (collection + weight scale pair) to (level-listings multimap + weight scale pair)
				.map(collectionWeightPair -> Tuple2.of(collectionWeightPair.getA().allLevelsAndListings(), collectionWeightPair.getB()))
				// For each level-listing multimap, scale and collect
				.forEach(multimapWeightPair -> multimapWeightPair.getA().keySet().forEach(
					// For each level,
					level -> res.table.putAll(Tuple3.of(entry.a, entry.b, level),
						// Scale each listing before collecting
						multimapWeightPair.getA().get(level).stream().map(listing -> new ScaledVanillaTradeListing(listing, multimapWeightPair.getB())).collect(Collectors.toList()))))
		);*/
		table.entries().stream().map(e -> Tuple3.of(e.getKey(), e.getValue().getA(), e.getValue().getB()))
			// Expand to {(key-profession), level, listing, weightScale}
			.flatMap(e -> e.b.allLevelsAndListings().entries().stream().map(levelAndListing -> Tuple4.of(e.a, levelAndListing.getKey(), levelAndListing.getValue(), e.c)))
			// Remove entries in which weight is 0 or negative
			.filter(e -> e.getD() > 0d)
			// Scale the weight to each listing
			.map(e -> Tuple4.of(e.a.getA(), e.a.getB(), e.b, new ScaledVanillaTradeListing(e.c, e.d)))
			// Put each scaled listing
			.forEach(e -> res.table.put(new Tuple3<>(e.a, e.b, e.c), e.d));

		collectedCache.setAndValidate(res);
		return res;
	}

	/**
	 * A pre-processed copy of a {@link VanillaTradeRegistry}, allowing to directly access the combined listing collections.
	 * Created only from {@link VanillaTradeRegistry#collect()}.
	 */
	public static class Collected {
		private final SetMultimap<Tuple3<ResourceLocation, VillagerProfession, Integer>, IVanillaTradeListing> table
				= HashMultimap.create();

		private Collected() {}

		/**
		 * Get a set of listings for given key, profession and merchant level.
		 */
		public Set<IVanillaTradeListing> get(ResourceLocation key, VillagerProfession profession, Integer level) {
			return table.get(Tuple3.of(key, profession, level));
		}

		/**
		 * Get a level-listings mapping for specified key and profession.
		 * @return A listing-collection-like mapping, unmodifiable but supporting most queries
		 * of {@link VanillaTradeListingCollection}.
		 */
		public IVanillaTradeListingCollection<IVanillaTradeListing> get(ResourceLocation key, VillagerProfession profession) {
			SetMultimap<Integer, IVanillaTradeListing> res = HashMultimap.create();
			table.keySet().stream().filter(ks -> ks.a.equals(key) && ks.b.equals(profession))
					.forEach(ks -> res.putAll(ks.c, table.get(ks)));
			return new UnmodifiableVanillaTradeListingCollection<>(res);
		}

		/**
		 * Get a set of listings for given key and merchant level for {@link VillagerProfession#NONE}.
		 */
		public Set<IVanillaTradeListing> getForDefaultProfession(ResourceLocation key, Integer level) {
			return this.get(key, VillagerProfession.NONE, level);
		}

		/**
		 * Get a level-listings mapping for specified key for {@link VillagerProfession#NONE}.
		 * @return A listing-collection-like mapping, unmodifiable but supporting most queries
		 * of {@link VanillaTradeListingCollection}.
		 */
		public IVanillaTradeListingCollection<IVanillaTradeListing> getForDefaultProfession(ResourceLocation key) {
			return this.get(key, VillagerProfession.NONE);
		}

		public Set<Tuple2<ResourceLocation, VillagerProfession>> keySet() {
			return table.keySet().stream().map(tp -> Tuple2.of(tp.a, tp.b)).collect(Collectors.toSet());
		}

	}

	/**
	 * Read data from a given location.
	 * @param data Location of data json file.
	 */
	public VanillaTradeRegistry readData(ResourceLocation data) {
		ResourceLocation oldLastKey = this.lastKey;
		VillagerProfession oldLastProf = this.lastProfession;
		// Data reading doesn't allow last key/prof actions
		this.lastKey = null;
		this.lastProfession = VillagerProfession.NONE;
		// Allow omitting ".json"
		ResourceLocation actualDataKey = data.toString().endsWith(".json")?
				data : new ResourceLocation(data.getNamespace(), data.getPath() + ".json");

		NFUDataStatics.readJsons(LogicalSide.SERVER, actualDataKey, json -> {
			try {
				json.getAsJsonArray().forEach(elem -> {
					ResourceLocation currentKey = null;	// For exception error information output
					try {
						/*
						Each array element stands for a registry entry, it can be either an object or a primitive.
						Object has format:
						{"key": "some_mod:some_key", "profession": "some_mod:some_profession", "collection": [...]}
						in which profession and collection can be omitted, and default profession is NONE.
						Primitive (string) stands for the key and anything else is default.
						Note that the collection with the identical key of the entry will be auto added.

						The "collection" argument is an array, of which the element is either
						primitive (string) or like: {"key": "some_mod:collection_key", "weight": 2.0}.
						If primitive, the value is string key and weight is 1.0.
						When "collection" is single-element, it can be simplified as a string primitive.
						*/
						// Read key
						ResourceLocation key = null;
						if (elem.isJsonObject())
							key = NFUDataStatics.getOptional(elem.getAsJsonObject(), "key", JsonElement::getAsString)
								.map(ResourceLocation::new).orElse(null);
						else if (elem.isJsonPrimitive())
							key = new ResourceLocation(elem.getAsString());
						if (key == null) return;
						currentKey = key;
						// Read profession
						VillagerProfession prof = VillagerProfession.NONE;
						if (elem.isJsonObject()) {
							prof = NFUDataStatics.getOptional(elem.getAsJsonObject(), "profession", JsonElement::getAsString)
								.map(ResourceLocation::new)
								.map(ForgeRegistries.VILLAGER_PROFESSIONS::getValue)
								.orElse(VillagerProfession.NONE);
						}

						// == Read collections == //
						Map<ResourceLocation, Double> collectionKeysAndWeights = new HashMap<>();
						if (elem.isJsonObject()) {
							// Resolve each collection and weight. Method getOptionalList handles primitive/object
							// as single-element array, so no need to distinguish them here
							NFUDataStatics.getOptionalList(elem.getAsJsonObject(), "collections", e -> {
								if (e.isJsonObject()) {
									String collectionKey = e.getAsJsonObject().get("key").getAsString();
									double weight = NFUDataStatics.getOptionalDouble(e.getAsJsonObject(), "weight")
										.orElse(1d);
									return Tuple2.of(collectionKey, weight);
								}
								else if (e.isJsonPrimitive())
									return Tuple2.of(e.getAsString(), 1d);
								else return null;
							}, e -> (e.isJsonObject() && e.getAsJsonObject().has("key")) || e.isJsonPrimitive())
								.stream().filter(Objects::nonNull)
								.forEach(entry -> collectionKeysAndWeights.put(new ResourceLocation(entry.getA()), entry.getB()));
						}
						else if (elem.isJsonPrimitive())
							collectionKeysAndWeights.put(new ResourceLocation(elem.getAsString()), 1d);
						if (!collectionKeysAndWeights.containsKey(key))	// Include the file with the same name by default
							collectionKeysAndWeights.put(key, 1d);
						var collectionsAndWeights = collectionKeysAndWeights.entrySet().stream()
							.map(entry -> Tuple2.of(NFURegistries.VANILLA_TRADE_LISTING_COLLECTIONS.getValue(entry.getKey()), entry.getValue()))
							.filter(entry -> entry.getA() != null).toList();
						for (var entry: collectionsAndWeights) {
							this.put(key, prof, entry.getA(), entry.getB());
						}
						// Set back last key/prof records. Data reading doesn't allow last key/prof actions
						this.lastKey = oldLastKey;
						this.lastProfession = oldLastProf;
					} catch (Exception e) {
						NFUDebugStatics.errorOnce(VanillaTradeRegistry.class,
								String.format("Read data failed, entry '%s' skipped. Exception: \n%s",
										Optional.ofNullable(currentKey).map(ResourceLocation::toString)
												.orElse("(missing key)"), e.getMessage()));
					}
				});
			} catch (Exception e){
				NFUDebugStatics.errorOnce(VanillaTradeRegistry.class,
						String.format("Read data failed, json '%s' skipped. Exception: \n%s", data.toString(), e.getMessage()));
			}
		});
		this.lastKey = oldLastKey;
		this.lastProfession = oldLastProf;
		return this;
	}

	// Utilities

	public boolean hasAnyListing(ResourceLocation key, VillagerProfession profession) {
		return !this.collect().get(key, profession).isEmpty();
	}

	public List<ResourceLocation> getAllCollectionKeys(ResourceLocation key, VillagerProfession profession) {
		return this.getCollections(key, profession).keySet()
			.stream().map(VanillaTradeListingCollection::getRegistryKey)
			.map(o -> o.orElse(new ResourceLocation("not:registered")))
			.collect(Collectors.toList());
	}

}