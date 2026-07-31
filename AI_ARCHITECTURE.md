# NFU Library — Architecture Overview (for AI agents)

This document is an AI-readable map of the NFU Library codebase. It describes the
high-level architecture of each sub-module and how it works, without going into
line-level detail. Use it to quickly orient yourself before diving into specific
packages.

> **Branch scope:** This document describes the **`1.20.1-0.2.33-de-legacy`**
> branch (mod version `0.2.33`). That version is a large refactor of the older
> `1.20.1` line: the **Entity Component System (ECS)** is now the primary state
> framework and most legacy Forge-capability subsystems (entity data, entity
> timer, mob-anger, vanilla-animal taming) have been **removed** in favor of it.
> The custom registry framework was also reworked. Where a construct was recently
> added, removed, or migrated, this document says so explicitly.

## What this project is

**NFU (Natrium Forge Utilities)** is a **Minecraft Forge library mod** (not a
standalone gameplay mod). It provides reusable utilities, APIs, and event hooks
that other Forge mods depend on to implement game mechanics. It ships no major
gameplay content of its own beyond a few debug items and internal test objects.

- **Mod id:** `nfulib` (legacy id: `nautils`; was previously part of `nffservices`)
- **Base package:** `net.sodiumzh.nfu`
- **Version:** `0.2.33` (branch `1.20.1-0.2.33-de-legacy`)
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

- **Component-driven (ECS-first).** Per-entity state and logic is attached through
  the **Entity Component System** built on a single Forge capability
  (`CEntityComponentManager`). The older standalone Forge capabilities for entity
  data, timers, and mob anger have been **removed**; the ECS is now the primary
  mechanism, with a handful of Forge capabilities remaining only for infrastructure
  (ticking, baubles).
- **Interface-prefix conventions.** `C`-prefixed types (e.g.
  `CEntityComponentManager`, `CBaubleEquippableMob`, `CEntityTickingCapability`)
  denote **Forge capability interfaces**; non-prefixed classes are their default
  implementations. Newer public contracts increasingly use an **`I`-prefix**
  (`IEntityComponent`, `IVanillaMerchant`, `IEntityComponentAccess`,
  `ITamingProcess`) — these are plain interfaces implemented by mobs/components,
  not capabilities.
- **`NFU...Statics` classes** in `util/` are stateless helper libraries — the
  primary "standard library" surface of the mod.
- **Supplier-based registration.** NFU's custom registries and Forge registrations
  both register `Supplier`s and resolve lazily.
- **Side-awareness via `AvailableSide`.** A dedicated enum (`network/AvailableSide`,
  `BOTH`/`SERVER`/`CLIENT`) is threaded through registries, capabilities, and
  components to declare on which logical side a value/component is valid; access on
  the wrong side is guarded (often returns `null` or throws `WrongSideException`).
- **Annotations as contracts.** `annotation/` holds marker annotations
  (`@DontOverride`, `@DontCallManually`, `@MustBeRegistered`, `@CapabilityInterface`,
  `@NotYetImplemented`, etc.) that document intended usage and constraints.
- **Foolproof wrappers** in `object/` (`Validatable`, `ServerOnly`, `ClientOnly`,
  `LimitedMutable`, `Upcastable`) guard against common misuse by throwing on
  incorrect access or by enabling safe self-upcasting in chainable builders.
- **Migration awareness.** The transition to the ECS is essentially complete on this
  branch: the deprecated capability APIs are **gone**, and `eventhandler/DataPort0x33`
  rewrites old save data on load (e.g. legacy `dynamic_data`→`data`,
  `default_timer`→`timer` components) so existing worlds keep working.

---

## Sub-modules

### 1. Custom Registry System — `registry/`
A lightweight, Forge-independent registry framework for registering arbitrary
custom data types by `ResourceLocation` key. It was **reworked** on this branch.
- `NFURegistry<T>` — a single registry; declared statically per data type. Entry
  values are `Supplier`-backed and become **immutable after the registry is loaded**
  (loading was previously called "generating"). Loading is now **mandatory** and
  happens at one of three configurable timings: **common setup**, **side setup**
  (client/server), or **first access**. A registry is `AvailableSide`-aware, supports
  load-order dependencies (`loadBefore`/`loadAfter`, cycle-checked), and keeps an
  internal **reverse map** (value→key) to make key-from-value lookups fast.
- `NFURegistryGenerateValuesEvent` — event hierarchy (`Common`/`Client`/`Server` ×
  `Before`/`After`) posted around registry loading so mods can inject or adjust
  generated values.
- `NFURegistryEntryCollection` — a `DeferredRegister`-like helper for declaring
  entries, returning `Accessor` handles (analogous to `RegistryObject`, side-aware);
  entries are committed via `merge()`.
- `NFURegistries` holds the "registry of registries" (data serializers, functions,
  predicates, vanilla-trade listings/collections/registries, mob-anger reasons/rules,
  entity-attribute providers, baubles, 3D field/inequality patterns, entity-component
  types, mob-applicable-item tables). Other files (`NFUItems`, `NFUEntityTypes`,
  `NFUEffects`, `NFUEntityComponents`, `NFUFunctions`, `NFUPredicates`, `NFUTags`,
  `NFUEntityDataSerializers`) declare the library's own built-in registered objects
  using both Forge and NFU registries. (The former `NFUCapabilities` and
  `NFUCapabilityAttachment` declarations were **removed** along with the deprecated
  capabilities.)
- `NFUConfigs` defines the Forge config spec and caches values. Current keys:
  `enablesSaveDataPorter` (save-data redirection toggle),
  `enablesFlyingSpeedScalingFix` (MC-172801 workaround), `debugMessageOutput`,
  `entityComponentHierarchyCheck` (periodic ECS hierarchy validation, debug only),
  and `crashedOnEntityLoadFailed`.

### 2. Entity Component System (ECS) — `entity/component/` (+ `preset/`)
The modern, **primary** framework for attaching modular state/logic to entities.
Built on a single Forge capability (`CEntityComponentManager`, impl
`CEntityComponentManagerImpl`) per entity, which itself is a `CEntityTickingCapability`.
- Each entity has one **component manager** acting as the root of a **tree of
  components** (`IEntityComponent` / `EntityComponentBase`). Components have a parent,
  named sub-components, and are addressed by **path** (slash-delimited, e.g.
  `/data`, `/timer`) or by type. They can declare **required sub-components** and
  **allowed paths**; the tree enforces these and prevents cycles (excessive depth
  throws). `EntityNodeComponent` is an inert placeholder used to fill intermediate
  path branches.
- **Three-phase setup.** Manager construction posts `EntityComponentSetupEvent`
  (listeners declaratively `addComponent(path, type, priority, side)` with ordering
  priorities), materializes the tree, then posts `EntityComponentFinalizeSetupEvent`
  for post-wiring. During construction only the sub-component accessors are valid;
  methods marked `@NotAvailableInManagerConstruction` (including
  `EntityComponentAPI.getComponentManager`) must not be called then.
  `EntityComponentManagerPlaceholder` is a no-op stand-in when a manager is absent.
- **Types & API.** `EntityComponentType` (record: entity class, component class,
  `AvailableSide`, factory) is registered in `NFURegistries.ENTITY_COMPONENT_TYPES`
  and enumerated by `EntityComponentTypes` (ROOT, DATA, TIMER, SYNCHER, NODE,
  ATTRIBUTE_MONITOR, ITEM_STACK_MONITOR, HEALING_HANDLER). `EntityComponentAPI` is the
  public entrypoint; `EntityComponentStatics` holds the manager capability;
  `SubComponentAccessor` bundles a path + type for type-safe lookup.
- **Lifecycle & persistence.** `EntityComponentEventListeners` attaches the manager,
  seeds default components, calls `joinLevel`, and drives per-tick sync. The manager
  ticks descendants (parent-before-child, filtered by `tickingSide()`), serializes the
  whole tree to entity NBT, and posts
  `EntityComponentManagerSerializeEvent`/`DeserializeEvent` (`Before`/`After`) around
  save/load. **State is not auto-synced**; sync is explicit (see below).
- **Preset components — `entity/component/preset/`:**
  - `EntityDataComponent` (path `/data`) — dynamic data store: per-side **transient**
    variables (not saved) plus **permanent** NBT-serialized variables (via
    `NFUDataSerializer`). Replaces the removed `CEntityDataCapability`.
  - `EntityTimerComponent` (path `/timer`) — general and per-UUID timers, delayed
    actions, looping, and expiry callbacks (override, `IEntityTimerComponentAccess`,
    or event). Replaces the removed `CEntityTimerCapability`.
  - `EntitySyncherComponent` — explicit server↔client synchronization: synched data
    keys and cached getters, manual `syncToClient`/`syncToServer`, per-tick dirty-key
    auto-sync, with packet IDs to discard stale/out-of-order packets.
  - `EntityAttributeMonitorComponent` / `EntityItemStackMonitorComponent` (server-side)
    — fire change notifications when watched attributes / item stacks change
    (override, the matching `IEntity...MonitorAccess` interface, or an event).
  - `EntityParticleHandlerComponent` — particle emission hook (placeholder).
  - `HealingHandlerComponent` — heal-by-item cooldown bookkeeping.
  - Access interfaces `IEntityComponentAccess` (and its
    `IEntityTimerComponentAccess` / `IEntityAttributeMonitorAccess` /
    `IEntityItemStackMonitorAccess` extensions) let an entity class react to component
    lifecycle and seed its own default tree.
  - `EntityComponentPresetClientPacketHandlers` / `...ServerPacketHandlers` handle the
    syncher's packets on each side.

### 3. Forge Capabilities (infrastructure) — `capability/`
The remaining Forge-capability layer, now pared down to infrastructure that the ECS
and other systems build on.
- `CEntityTickingCapability` — a capability auto-ticked each entity tick (driven by
  NFU's mixins) on the side(s) reported by its `AvailableSide`; the ECS manager is one.
- `CapabilityObject` — a type-safe polymorphic wrapper over an `Entity`, `BlockEntity`,
  or `Level` with fluent `is*/as*/if*` accessors, used where a capability holder may be
  any of the three.
- Provider helpers: `NFUCapProvider`, `NFUEntityCapProvider`, and
  `NFUEntitySerializableCapProvider` standardize capability attach + optional NBT
  persistence.
- **Removed on this branch:** `CEntityDataCapability`, `CEntityTimerCapability`,
  `EntityTimerAccessor`, `NFUCapsEventListeners`, and `SerializableCapabilityProvider`.
  Their functionality now lives in the ECS `EntityDataComponent` (`/data`) and
  `EntityTimerComponent` (`/timer`).

### 4. Mob Anger System — `entity/anger/`
A framework for per-target "anger" (aggro) with decay, forgiveness, and rules. On
this branch it is **purely component-based** — the old capability classes
(`CMobAngerHandler`, `MobAngerHandler`, `ConditionalNeutralMob`,
`CConditionalNeutralMob`) have been **removed**.
- `MobAngerHandlerComponent` (an ECS component on `Mob`) tracks remaining anger ticks
  per target UUID and ticks them down, persisting to NBT. A mob opts in by
  implementing `IUsesDefaultAngerHandler`, which exposes the default handler
  (registered as the `DEFAULT_ANGER_HANDLER` component type at path
  `/default_anger_handler`) with convenience get/set/forgive methods.
- `MobAngerRules` (a custom-registry type) defines how a mob becomes/stays angry and
  for how long; `MobAngerReason` (registry type) classifies causes.
- Result/enum types (`MobForgiveResult`, `MobSetAngerResult`, `MobAngryAtEvent`,
  `MobAngerRulesEvent`) and `MobAngerEventListeners` wire it into gameplay events.

### 5. Vanilla Trade API — `entity/vanillatrade/`
Enables vanilla villager-style trading on arbitrary mobs.
- `IVanillaMerchant` (interface, renamed from the former `CVanillaMerchant`) makes a
  mob a vanilla `Merchant`. It has two implementations: `VanillaMerchant` (standalone
  default impl) and the newer `VanillaMerchantComponent` (an **ECS component** — the
  recommended, component-based way to attach merchant behavior). `IVanillaTradeListing`
  + implementations (`VanillaTradeListing`, `ScaledVanillaTradeListing`,
  `VanillaTradeListingEnchanted`) describe individual offers.
- Listing collections (`IVanillaTradeListingCollection`, `VanillaTradeListingCollection`,
  `ExtendableVanillaTradeOfferList`, `UnmodifiableVanillaTradeListingCollection`,
  `VanillaTradeListingCollectionHelper` + its event) build randomized/weighted offer
  sets; `RandomEnchantmentSelector` supports enchanted-book style trades.
- `VanillaTradeRegistry` (+ `VanillaTradeRegistryEvent`) registers trade tables
  globally; listings/collections/registries are NFU custom-registry types.

### 6. Mob Taming API — `entity/taming/`
Interfaces defining taming processes on mobs (with or without progress tracking). The
concrete vanilla-animal implementation and its event plumbing
(`VanillaAnimalTamingProcess`, `CVanillaAnimalTamingProcessHandler`,
`IUsesTamingProcess`, `TamingProcessEventListeners`) were **removed** on this branch,
leaving the specification surface:
- `ITamingProcess` / `ITamingProcessWithProgress` define taming behavior; processes are
  typically **stateless singletons** operating on external player/mob state, with the
  optional progress variant tracking a 0–1 value per player/mob pair.
- `TamingInteractionResult` wraps the interaction outcome (and any newly-tamed mob).

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
- `mixin/mixin/` — the actual Mixin classes (targeting `Entity`, `EntityType`,
  `LivingEntity`, `Mob`, `EnderMan`, `Projectile`, `ItemStack`, `ItemEntity`,
  `ThrownTrident`, `ThrownPotion`, `AreaEffectCloud`, `MerchantMenu`, `GrindstoneMenu`,
  `Player`, `ServerPlayer`, `ServerPlayerGameMode`, `ServerLevel`, `ResourceLocation`,
  etc., plus client mixins for merchant screen / renderer / client level). Listed in
  `nfulib.mixins.json`. `NFUMixinEntityType` cooperates with the save-data redirector
  to port entity-type keys on load.
- `mixin/event/` — the custom event classes posted by those mixins, grouped by
  `entity/`, `item/`, `level/`, and `client/`. Examples: `NonLivingEntityHurtEvent`,
  `ProjectileHitEvent`, `MobInteractEvent`, `MobCheckDespawnEvent`,
  `LivingStartDeathEvent`, `LootCheckPlayerKillEvent`, `BlockItemConsumeOnPlacedEvent`,
  and the `LivingEntity.aiStep` hooks `LivingStartAiStepEvent` / `LivingFinishAiStepEvent`
  (plus `LivingStartBaseAiStepEvent` / `LivingFinishBaseAiStepEvent`, the latter renamed
  from `LivingEndBaseAiStepEvent`). The old generic `EntityTickEvent` was **removed**
  (per-tick logic now flows through the ticking capability / ECS). Many events are
  cancellable or carry a result.
- These events are the recommended way for dependent mods to intercept vanilla
  behavior not otherwise exposed by Forge.

### 9. Networking — `network/` (+ `network/packet/`)
Thin wrapper over Forge's `SimpleChannel`.
- `NFUNetworkChannels` sets up the channel. `network/packet/` holds the packet types
  (`ClientboundEntityMotionUpdatePacket`, `ClientboundLivingSyncEquipmentPacket`) and
  `NFUClientboundPacketHandlers` (moved here from `network/`) handles client-side
  reception. (ECS syncher packets are handled separately in
  `entity/component/preset/`.)
- `NFUDataSerializer` / `NFUDataSerializers` provide custom `EntityDataSerializer`s
  (registered via `NFUEntityDataSerializers`) and buffer/NBT conversion for syncing
  custom types (e.g. `Vec3`, `LinearColor`, bounding boxes, 3D fields/inequalities).
- `AvailableSide` — the shared server/client-availability enum used across the mod
  (see conventions above).

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
`Tag`, `AI`, `Compat`, `Info`, `Misc`, `Debug`. On this branch `NFUMathStatics` gained
3D geometry and **parabolic-trajectory** helpers (fixed-speed / fixed-direction), and
`NFUReflectionStatics` was reworked to use the cached reflection *searchers* (below).

### 15. Data Structures & Math — `container/`, `math/`, `function/`
- `container/` — general data structures: tuples (`Tuple2/3/4`), 2D tables
  (`ITable2D`/`Table2D`, plus the new `ImmutableTable2D`), linkable collections
  (`LinkableSet`, `LinkableMultimap`), `AppendedMap`, `CyclicSwitch`. (The older
  `ArrayIterable`, `MapPair`, and `NaUtilsImmutableMap` were **removed**.)
- `math/` — randomness and math utilities: weighted/ranged random selection,
  3D fields/inequalities (`Field3D`, `Inequality3D`), color models
  (`LinearColor`, `HtmlColors`, `WithDyeColors`), `ThreadSafeRandomSource`, `GuiPos`.
  (The old `DoubleRandomSelection` was **removed**.)
- `function/` — chainable/extensible functional interfaces (`ChainablePredicate`,
  `ChainableBiPredicate`, `ChainableUnaryOperator`, `ModifiableSupplier`,
  `MutablePredicate`, and the argument-carrying `UnaryOperatorOneArg` /
  `UnaryOperatorTwoArgs`) plus **registrable** functions/predicates
  (`RegistrableFunction`, `RegistrablePredicate`) that can be stored in NFU registries.

### 16. Reflection & Objects — `reflection/`, `object/`
- `reflection/` — cached reflective accessors (`CachedFieldAccessor`,
  `CachedMethodAccessor`) for performant repeated field/method access, plus the new
  thread-local **searchers** (`CachedFieldSearchers`, `CachedMethodSearchers`) that
  cache field/method lookups by class+name and offer safe `invokeIfPresent`-style calls.
- `object/` — object-oriented utilities: foolproof wrappers (`Validatable`,
  `ServerOnly`, `ClientOnly`, `LimitedMutable`), casting helpers
  (`ICastable`/`CastableObject`), the `Upcastable` self-upcasting mixin for chainable
  builders, `HierarchyPath` (an immutable slash-delimited path type used for
  component-tree navigation), chain-modifiable pattern, `FilteredMapper`,
  `DirectedGraphNode`.

### 17. Events & Handlers — `event/`, `eventhandler/`
- `event/` — base classes for NFU's own (non-mixin) event hierarchy
  (`NFUEntityEvent`, `NFULivingEvent`, `NFUObjectEvent`).
- `eventhandler/` — Forge event subscribers that host the library's runtime logic:
  `NFUEntityEventHandlers`, `NFUServerEventHandlers`, `NFUSetupEventHandlers`, and
  `DataPort0x33` — a save-data migration handler that rewrites legacy ECS component
  NBT on entity load (e.g. `dynamic_data`→`data`, `default_timer`→`timer`).

### 18. Client — `client/`
Client-only setup and GUI helpers: `NFUClientSetupEventListeners`, `NFUGUIStatics`,
and `client/renderer/` for custom rendering. Client mixins live under `mixin/mixin/`
and are listed in the `client` section of `nfulib.mixins.json`.

### 19. Compatibility & Misc — `compat/`, `annotation/`, `exception/`, `info/`, `level/`
- `compat/` — optional mod-dependency handling (`ModDependent`,
  `ModDependencyFallbackItem`).
- `annotation/` — contract/marker annotations (see conventions above).
- `exception/` — typed exceptions: `WrongSideException`, `ReflectionFailedException`,
  `AssertionFailedException`, `IllegalGenericsException`, `MissingInterfaceException`,
  `UnimplementedException`, `InfiniteRecursionException`,
  `DuplicateRegistryEntryException`, and `MissingRegistryEntryException` (renamed from
  the former `MissingRegistryException`).
- `info/ComponentBuilder` — text `Component` building helper.
- `level/HitResultInfo` — a context-independent, serializable record capturing a
  `HitResult` (block or entity) so hit data can be stored/sent across sides.
  (The former `component/TickableLevelContext` was **removed**.)

---

## How the pieces fit together (runtime flow)

1. **Bootstrap:** `NFULibrary` constructor registers Forge `DeferredRegister`s,
   merges NFU custom registries, and initializes the ECS, bauble system, and
   save-data redirector.
2. **Registration events:** Dependent mods and NFU itself declare registered objects
   via Forge registries and `NFURegistryEntryCollection`, plus subsystem-specific
   register/modify events (baubles, trades, anger rules, component types). NFU
   registries then **load** (making values immutable) at their configured timing —
   common setup, side setup, or first access — bracketed by
   `NFURegistryGenerateValuesEvent`.
3. **Attachment:** The ECS manager (and remaining infrastructure capabilities) are
   attached to entities on creation via the provider classes; components are added
   during the `EntityComponentSetupEvent`/`FinalizeSetupEvent` phases.
4. **Runtime hooks:** NFU **mixins** post custom events into vanilla code paths;
   `eventhandler/` and per-subsystem listeners react to them, driving ticking,
   anger decay, taming, bauble behavior, trades, etc. Components tick through the
   ticking capability; server↔client sync is explicit via `EntitySyncherComponent`.
5. **Persistence:** Components/capabilities serialize to entity/level NBT;
   `DataPort0x33` upgrades legacy component NBT and `SaveDataLocationRedirector`
   rewrites legacy registry keys on load.

## Where to look first for common tasks

- Add a new custom registered type → `registry/` (`NFURegistry` + `NFURegistryEntryCollection`).
- Hook a vanilla behavior with no Forge event → check `mixin/event/`; add a mixin in `mixin/mixin/`.
- Attach new per-entity state/logic → Entity Component System (`entity/component/`;
  ready-made components in `entity/component/preset/`).
- Store per-entity data or run timers → the `/data` and `/timer` preset components.
- Sync entity state to clients → `EntitySyncherComponent`.
- Utility method for entities/items/NBT/math → the matching `util/NFU*Statics` class.
- Villager-style trading on a mob → `entity/vanillatrade/`.
- Equip items on mobs → `item/bauble/`.
- Aggro / anger logic → `entity/anger/` (`MobAngerHandlerComponent`,
  `IUsesDefaultAngerHandler`).

> Note: `wiki/NFU Library.md` is the human-facing manual (with usage snippets) but is
> partially incomplete. This file is the authoritative architectural map for AI agents.
