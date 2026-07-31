# NFU Library — Architecture Overview (for AI agents)

This document is an AI-readable map of the NFU Library codebase. It describes the
high-level architecture of each sub-module and how it works, without going into
line-level detail. Use it to quickly orient yourself before diving into specific
packages.

## What this project is

**NFU (Natrium Forge Utilities)** is a **Minecraft Forge library mod** (not a
standalone gameplay mod). It provides reusable utilities, APIs, and event hooks
that other Forge mods depend on to implement game mechanics. It ships no major
gameplay content of its own beyond a few debug items and internal test objects.

- **Mod id:** `nfulib` (legacy id: `nautils`; was previously part of `nffservices`)
- **Base package:** `net.sodiumzh.nfu`
- **Target:** Minecraft `1.20.1`, Forge `47.x`, Java 17
- **Mappings:** Parchment
- **Key dependencies:** MixinExtras (bundled via jarJar), JEI (compile/runtime API only)
- **License:** LGPL-3.0

The mod entry point is `NFULibrary` (`@Mod("nfulib")`). Its constructor wires up
everything: registers Forge `DeferredRegister`s, merges NFU custom registries,
initializes the entity-component and bauble systems, and configures save-data
redirection. There is no gameplay logic in the entry class itself — it is pure
bootstrap.

## Source layout

- `src/main/java/net/sodiumzh/nfu/**` — all library code, grouped by subsystem (below).
- `src/main/resources/META-INF/mods.toml` — Forge mod metadata (templated by Gradle).
- `src/main/resources/nfulib.mixins.json` — Mixin config (common + client mixin lists).
- `src/main/resources/assets|data/nfulib/**` — textures, models, lang, tags for built-in objects.
- `wiki/` — human-facing documentation (partially incomplete).

## Cross-cutting design conventions

- **Capability + Component driven.** Most per-entity state is attached through
  Forge Capabilities, increasingly wrapped by the newer **Entity Component System**.
- **`C`-prefixed interfaces** (e.g. `CVanillaMerchant`, `CBaubleEquippableMob`,
  `CMobAngerHandler`) denote **capability interfaces**; non-prefixed classes are
  their default implementations.
- **`NFU...Statics` classes** in `util/` are stateless helper libraries — the
  primary "standard library" surface of the mod.
- **Supplier-based registration.** NFU's custom registries and Forge registrations
  both register `Supplier`s and resolve lazily.
- **Annotations as contracts.** `annotation/` holds marker annotations
  (`@DontOverride`, `@DontCallManually`, `@MustBeRegistered`, `@CapabilityInterface`,
  `@NotYetImplemented`, etc.) that document intended usage and constraints.
- **Foolproof wrappers** in `object/` (`Validatable`, `ServerOnly`, `ClientOnly`,
  `LimitedMutable`) guard against common misuse by throwing on incorrect access.
- **Migration awareness.** Several older APIs (`MobAngerHandler`,
  `CEntityDataCapability`, `CEntityTimerCapability`) are `@Deprecated` in favor of
  the Entity Component System; both coexist during transition.

---

## Sub-modules

### 1. Custom Registry System — `registry/`
A lightweight, Forge-independent registry framework for registering arbitrary
custom data types by `ResourceLocation` key.
- `NFURegistry<T>` — a single registry; declared statically per data type. Values
  are `Supplier`-backed and resolved lazily (or eagerly at a configurable setup phase).
- `NFURegistryEntryCollection` — a `DeferredRegister`-like helper for declaring
  entries, returning `Accessor` handles (analogous to `RegistryObject`); entries
  are committed via `merge()`.
- `NFURegistries` holds the "registry of registries". Other files
  (`NFUItems`, `NFUEntityTypes`, `NFUEffects`, `NFUCapabilities`, `NFUEntityComponents`,
  `NFUFunctions`, `NFUPredicates`, `NFUConfigs`, `NFUTags`, `NFUEntityDataSerializers`,
  `NFUCapabilityAttachment`) declare the library's own built-in registered objects
  using both Forge and NFU registries.
- `NFUConfigs` defines the Forge config spec (save-data porter toggle, debug mode,
  flying-speed-fix toggle) and caches values.

### 2. Entity Component System (ECS) — `entity/component/`
The modern, primary framework for attaching modular state/logic to entities. Built
on top of a single Forge capability (`CEntityComponentManager`) per entity.
- Each entity has one **component manager** acting as the root of a **tree of
  components** (`IEntityComponent` / `EntityComponentBase`). Components can hold
  data, declare required sub-components, and be navigated by path or type.
- The manager handles ticking, NBT serialization, requirement checks, and cycle
  prevention. Server and client each maintain their own tree; **state is not auto
  synced** — sync is explicit via `EntitySyncherComponent` / packet handlers.
- Built-in component types (registered in `EntityComponentTypes`): dynamic data
  (`EntityDynamicDataComponent`), timers (`EntityTimerComponent`), particle handling,
  node/graph, syncher. `EntityComponentAPI` is the public entrypoint.
- `EntityComponentEventListeners` / `EntityComponentSetupEvent` integrate the tree
  with entity lifecycle events (attach, tick, load/save).

### 3. Forge Capabilities (legacy/support) — `capability/`
Reusable capability templates and providers that predate or complement the ECS.
- `CEntityTickingCapability` — a capability auto-ticked each entity tick (driven by
  NFU's mixin `EntityTickEvent`).
- `CEntityDataCapability` (deprecated) — generic NBT data container with transient
  (non-serialized) parameter support.
- `CEntityTimerCapability` / `EntityTimerAccessor` — countdown/timer capability.
- Provider helpers: `NFUCapProvider`, `NFUEntityCapProvider`,
  `SerializableCapabilityProvider`, and their serializable variants standardize
  capability attach + NBT persistence.

### 4. Mob Anger System — `entity/anger/`
A framework for per-target "anger" (aggro) with decay, forgiveness, and rules.
- `CMobAngerHandler` (capability) / `MobAngerHandlerComponent` (ECS successor,
  `MobAngerHandler` is deprecated) track anger levels per player UUID and tick them down.
- `MobAngerRules` (a custom-registry type) defines how a mob becomes/stays angry;
  `MobAngerReason` classifies causes. `ConditionalNeutralMob` /
  `CConditionalNeutralMob` model neutral mobs that turn hostile under conditions.
- Result/enum types (`MobForgiveResult`, `MobSetAngerResult`, `MobAngryAtEvent`,
  `MobAngerRulesEvent`) and `MobAngerEventListeners` wire it into gameplay events.

### 5. Vanilla Trade API — `entity/vanillatrade/`
Enables vanilla villager-style trading on arbitrary mobs.
- `CVanillaMerchant` (capability) / `VanillaMerchant` (default impl) make a mob a
  merchant. `IVanillaTradeListing` + implementations (`VanillaTradeListing`,
  `ScaledVanillaTradeListing`, `VanillaTradeListingEnchanted`) describe individual offers.
- Listing collections (`VanillaTradeListingCollection`, `Extendable...`,
  `Unmodifiable...`, `...CollectionHelper`) build randomized/weighted offer sets;
  `RandomEnchantmentSelector` supports enchanted-book style trades.
- `VanillaTradeRegistry` (+ events) registers trade tables globally.

### 6. Mob Taming API — `entity/taming/`
A framework for taming processes on mobs (with or without progress tracking).
- `ITamingProcess` / `ITamingProcessWithProgress` define taming behavior; processes
  are typically **stateless singletons** that operate on external player/mob state.
- `VanillaAnimalTamingProcess` + `CVanillaAnimalTamingProcessHandler` implement
  vanilla-animal-like taming; `IUsesTamingProcess`, `TamingInteractionResult`, and
  `TamingProcessEventListeners` connect it to interaction events.

### 7. Bauble (Mob Equipment) System — `item/bauble/`
Curios-like system for equipping "bauble" items onto **mobs** (not players).
- `CBaubleEquippableMob` (capability, with impl/empty/provider variants) holds a
  mob's bauble slots; `NFUBaubleAPI` is the static entrypoint.
- `BaubleBehavior` / `DedicatedBaubleItem` / `IBaubleRegistryEntry` define what a
  bauble does; `BaubleAttributeModifier` applies attribute effects.
- `BaubleEquippingCondition(s)` (custom-registry types) gate which mobs can wear
  which baubles. A set of registry/setup events
  (`RegisterBaublesEvent`, `RegisterBaubleEquippableMobsEvent`, `Modify...Event`,
  `BaubleEquippableMobTickEvent`) drive registration and per-tick behavior.

### 8. Mixin-based Event Hooks — `mixin/`
The largest and most important integration surface: injects NFU custom events into
vanilla code where Forge lacks hooks.
- `mixin/mixin/` — the actual Mixin classes (targeting `Entity`, `LivingEntity`,
  `Mob`, `Projectile`, `ItemStack`, `ItemEntity`, `ThrownTrident`, `MerchantMenu`,
  `GrindstoneMenu`, `AreaEffectCloud`, `ThrownPotion`, `Player`, `ServerLevel`, etc.,
  plus client mixins for merchant screen / renderer). Listed in `nfulib.mixins.json`.
- `mixin/event/` — the ~40 custom event classes posted by those mixins, grouped by
  `entity/`, `item/`, `level/`, and `client/`. Examples: `EntityTickEvent`,
  `NonLivingEntityHurtEvent`, `ProjectileHitEvent`, `MobInteractEvent`,
  `MobCheckDespawnEvent`, `LivingStartDeathEvent`, `LootCheckPlayerKillEvent`,
  `BlockItemConsumeOnPlacedEvent`. Many are cancellable or carry a result.
- These events are the recommended way for dependent mods to intercept vanilla
  behavior not otherwise exposed by Forge.

### 9. Networking — `network/`
Thin wrapper over Forge's `SimpleChannel`.
- `NFUNetworkChannels` sets up the channel; `network/packet/` holds packet types;
  `NFUClientboundPacketHandlers` handles client-side reception.
- `NFUDataSerializer` / `NFUDataSerializers` provide custom `EntityDataSerializer`s
  (registered via `NFUEntityDataSerializers`) for syncing custom types in entity data.

### 10. Item Template — `item/`
A templating layer for items with extra capabilities (some backed by mixin).
- `INFUItem` is the marker interface external code casts to. `NFUItem` /
  `NFUBlockItem` are base classes providing: chainable tooltip/foil/name styling at
  construction, default-instance manipulation (override/redirect/remove, `/give`
  redirection), forced consumption in creative, and a "foolproof" simplified
  interaction signature.
- `ColoredItems` supports dye-colored item variants.

### 11. Built-in Items & Debug Tools — `item/debug/`, `effect/`, `block/`
- `item/debug/` — `/give`-only debug items: AI Switch (toggle mob AI), Target Setter
  (force a mob's attack target), Mob Remover (delete/kill mode), Tag Displayer.
- `effect/EmptyEffect` — a placeholder mob effect. `block/` — block helpers
  (`BlockMaterial`, `ColoredBlocks`, `SetBlockFlag`).

### 12. Entity Utilities & Helpers — `entity/` (root), `entity/ai/`
Miscellaneous entity building blocks not tied to one subsystem: attribute helpers
(`DeferredEntityAttributes`, `ConditionalAttributeModifier`, `AttributeModifierSwitch`,
`EntityAttributeProvider` + `DeferredEntityAttributeRegisterEvent` for attributes
that depend on server-load data), custom entities
(`NFUItemProjectileEntity`, `NFUEffectZoneEntity`, `AttachedItemDisplayerEntity`),
mob helpers (`MobApplicableItemTable`, `MobRespawnInfo`, `ManualTimer`,
`ServerEntityMotion`), and AI goals (`GoalGroup`, `NFURangedAttackGoal`).

### 13. Save-Data Redirection — `savedata/redirector/`
Migration utility that rewrites registry keys in existing world save data when a
mod renames its objects (used to migrate the legacy `nautils`/`nffservices` keys to
`nfulib`). `SaveDataLocationRedirector` (chainable singleton) maps old→new
namespaces/keys; toggled by the `enablesSaveDataPorter` config. Driven by setup and
level-load event listeners in the same package.

### 14. Standard Library / Static Helpers — `util/`
Stateless helper collections — the mod's general-purpose "stdlib". Each
`NFU<Domain>Statics` class groups methods by domain: `Container`, `Data`, `Entity`,
`Item`, `Level`, `Math`, `NBT`, `Network`, `Particle`, `Reflection`, `Registration`,
`Tag`, `AI`, `Compat`, `Info`, `Misc`, `Debug`.

### 15. Data Structures & Math — `container/`, `math/`, `function/`
- `container/` — general data structures: tuples (`Tuple2/3/4`), 2D tables
  (`Table2D`/`ITable2D`), linkable/immutable collections, `AppendedMap`,
  `MapPair`, `CyclicSwitch`, `ArrayIterable`.
- `math/` — randomness and math utilities: weighted/ranged random selection,
  3D fields/inequalities (`Field3D`, `Inequality3D`), color models
  (`LinearColor`, `HtmlColors`, `WithDyeColors`), `ThreadSafeRandomSource`, `GuiPos`.
- `function/` — chainable/extensible functional interfaces (`ChainablePredicate`,
  `ChainableUnaryOperator`, `ModifiableSupplier`, `MutablePredicate`) plus
  **registrable** functions/predicates (`RegistrableFunction`, `RegistrablePredicate`)
  that can be stored in NFU registries.

### 16. Reflection & Objects — `reflection/`, `object/`
- `reflection/` — cached reflective accessors (`CachedFieldAccessor`,
  `CachedMethodAccessor`) for performant repeated field/method access.
- `object/` — object-oriented utilities: foolproof wrappers (`Validatable`,
  `ServerOnly`, `ClientOnly`, `LimitedMutable`), casting helpers
  (`ICastable`/`CastableObject`), chain-modifiable pattern, `FilteredMapper`,
  `DirectedGraphNode`.

### 17. Events & Handlers — `event/`, `eventhandler/`
- `event/` — base classes for NFU's own (non-mixin) event hierarchy
  (`NFUEntityEvent`, `NFULivingEvent`, `NFUObjectEvent`).
- `eventhandler/` — Forge event subscribers that host the library's runtime logic:
  `NFUEntityEventHandlers`, `NFUServerEventHandlers`, `NFUSetupEventHandlers`.

### 18. Client — `client/`
Client-only setup and GUI helpers: `NFUClientSetupEventListeners`, `NFUGUIStatics`,
and `client/renderer/` for custom rendering. Client mixins live under `mixin/mixin/`
and are listed in the `client` section of `nfulib.mixins.json`.

### 19. Compatibility & Misc — `compat/`, `annotation/`, `exception/`, `info/`, `component/`
- `compat/` — optional mod-dependency handling (`ModDependent`,
  `ModDependencyFallbackItem`).
- `annotation/` — contract/marker annotations (see conventions above).
- `exception/` — typed exceptions (`WrongSideException`, `MissingRegistryException`,
  `ReflectionFailedException`, `AssertionFailedException`, etc.).
- `info/ComponentBuilder` — text `Component` building helper.
- `component/TickableLevelContext` — a per-level ticking context object (distinct
  from the entity-component system).

---

## How the pieces fit together (runtime flow)

1. **Bootstrap:** `NFULibrary` constructor registers Forge `DeferredRegister`s,
   merges NFU custom registries, and initializes the ECS, bauble system, and
   save-data redirector.
2. **Registration events:** Dependent mods and NFU itself declare registered objects
   via Forge registries and `NFURegistryEntryCollection`, plus subsystem-specific
   register/modify events (baubles, trades, anger rules, components).
3. **Attachment:** Capabilities/components are attached to entities on creation via
   the provider classes and ECS manager.
4. **Runtime hooks:** NFU **mixins** post custom events into vanilla code paths;
   `eventhandler/` and per-subsystem listeners react to them, driving ticking,
   anger decay, taming, bauble behavior, trades, etc.
5. **Persistence:** Capabilities/components serialize to entity/level NBT;
   `SaveDataLocationRedirector` rewrites legacy keys on load.

## Where to look first for common tasks

- Add a new custom registered type → `registry/` (`NFURegistry` + `NFURegistryEntryCollection`).
- Hook a vanilla behavior with no Forge event → check `mixin/event/`; add a mixin in `mixin/mixin/`.
- Attach new per-entity state/logic → Entity Component System (`entity/component/`).
- Utility method for entities/items/NBT/math → the matching `util/NFU*Statics` class.
- Villager-style trading on a mob → `entity/vanillatrade/`.
- Equip items on mobs → `item/bauble/`.
- Aggro/neutral-mob logic → `entity/anger/`.

> Note: `wiki/NFU Library.md` is the human-facing manual (with usage snippets) but is
> partially incomplete. This file is the authoritative architectural map for AI agents.
