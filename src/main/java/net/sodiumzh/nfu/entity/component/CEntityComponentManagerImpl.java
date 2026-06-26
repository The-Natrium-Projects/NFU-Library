package net.sodiumzh.nfu.entity.component;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.capability.CEntityTickingCapability;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.entity.component.preset.IEntityComponentAccess;
import net.sodiumzh.nfu.object.HierarchyPath;
import net.sodiumzh.nfu.object.Validatable;
import net.sodiumzh.nfu.registry.NFUConfigs;
import net.sodiumzh.nfu.registry.NFURegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Default implementation for CEntityComponentManager.
 * Acts as root node and manages child components. Root's parent is always null.
 * Handles ticking the whole component tree: each component is ticked exactly once, in parent-before-children order.
 */
final class CEntityComponentManagerImpl extends EntityComponentBase<Entity> implements CEntityComponentManager {

    private boolean constructionDone = false;
    private final Validatable<Map<HierarchyPath, IEntityComponent<?>>> preConstructed = new Validatable<>(new HashMap<>());    // Valid only in construction. Invalidated after construction.

    CEntityComponentManagerImpl(Entity entity) {
        super(entity);
        this.construct();
    }

    private void construct() {
        // Enter construction phase, collect component construction info by event
        preConstructed.validate();
        EntityComponentSetupEvent initEvent = new EntityComponentSetupEvent(this.getEntity(), this);
        initEvent.sharePreConstructedMapFrom(this.preConstructed.get());  // Share the map reference to event, so that it can be filled during event post
        MinecraftForge.EVENT_BUS.post(initEvent);
        // First pre-construct needed components and collect the result
        preConstructed.get().putAll(initEvent.preConstruct());
        // Then do actual collection while the pre-constructed components are available
        initEvent.collect();
        // End construction, disable transient variables
        preConstructed.get().clear();
        preConstructed.invalidate();
        this.constructionDone = true;
        // Post-construction hooks
        if (this.getEntity() instanceof IEntityComponentAccess holder)
            holder.initializeComponents(this);
        MinecraftForge.EVENT_BUS.post(new EntityComponentFinalizeSetupEvent(this.getEntity(), this));
        // Debug check, set this config true if debugging, and false in release to save resource
        if (NFUConfigs.CACHED_ENTITY_COMPONENT_HIERARCHY_CHECK)
            initEvent.checkHierarchy();
    }

    @Override
    public Optional<IEntityComponent<?>> getParent() {
        return Optional.empty();
    }

    // Enable pre-constructed component access

    @Override
    public Optional<IEntityComponent<? extends Entity>> getSubComponent(String name) {
        var res = super.getSubComponent(name);
        if (res.isPresent()) return res;
        else if (!this.constructionDone) {
            return Optional.ofNullable(this.preConstructed.get().get(HierarchyPath.byNameArray(name)));
        }
        else return Optional.empty();
    }

    @Override
    public Map<String, IEntityComponent<? extends Entity>> getSubComponents() {
        var res = super.getSubComponents();
        if (!this.constructionDone) {
            this.preConstructed.get().entrySet().stream()
                .filter(entry -> entry.getKey().length() == 1 && !res.containsKey(entry.getKey().getAt(0)))
                .forEach(entry -> res.put(entry.getKey().getAt(0), entry.getValue()));
        }
        return res;
    }

    @Override
    public Optional<IEntityComponent<? extends Entity>> getSubComponentByPath(HierarchyPath path) {
        // Half-constructed hierarchy may be half-available as each node ensures upstream to be available, so search normally in any case
        var res = super.getSubComponentByPath(path);
        if (res.isPresent()) return res;
        // If not found in construction phase, try finding pre-constructed components from transient map
        else if (!this.constructionDone)
            return Optional.ofNullable(this.preConstructed.get().get(path));
        else return Optional.empty();
    }

    @Override
    public void attachTo(@Nullable IEntityComponent<?> parent, String name) {
        throw new UnsupportedOperationException("CEntityComponentManager must be root and cannot attach to anything.");
    }

    @Override
    public void detachFromParent() {
    }

    /**
     * Handles ticking the whole component tree: each component is ticked exactly once, in parent-before-children order.
     */
    @Override
    public void tick() {
        this.getDownstreamComponents().stream().filter(IEntityComponent::shouldTick).forEach(IEntityComponent::tick);
        if (NFUConfigs.CACHED_ENTITY_COMPONENT_HIERARCHY_CHECK && this.getEntity().tickCount % 100 == 0) {
            this.getDownstreamComponents().stream().filter(c -> c instanceof EntityComponentBase<? extends Entity>)
                .map(c -> (EntityComponentBase<?>)c).forEach(EntityComponentBase::checkHierarchy);
        }
    }

    @Override
    public EntityComponentType<Entity, CEntityComponentManager> getType() {
        return EntityComponentTypes.ROOT.get();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        MinecraftForge.EVENT_BUS.post(new EntityComponentManagerSerializeEvent.Before(this.getEntity(), nbt));
        this.getSubComponents().entrySet().stream().filter(entry -> entry.getValue().shouldSerialize())
            .forEach(entry -> nbt.put(entry.getKey(), serializeComponent(entry.getKey(), entry.getValue())));
        MinecraftForge.EVENT_BUS.post(new EntityComponentManagerSerializeEvent.After(this.getEntity(), nbt));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        MinecraftForge.EVENT_BUS.post(new EntityComponentManagerDeserializeEvent.Before(this.getEntity(), nbt));
        nbt.getAllKeys().stream().map(nbt::getCompound).filter(t -> !t.isEmpty())
            .map(t -> new Tuple2<>(t.getString("name"), deserializeOrRebuildComponent(this.getEntity(), this.getSubComponent(t.getString("name")).orElse(null), t)))
            .forEach(tp -> {
                // If the component is absent, add it
                if (this.getSubComponent(tp.getA()).isEmpty())
                    this.addSubComponent(tp.getA(), tp.getB());
                    // this means occupied by a component of wrong type, and shouldn't happen
                else if (this.getSubComponent(tp.getA(), tp.getB().getType()).isEmpty())
                    throw new IllegalStateException("Component type conflict: deserializing subcomponent /" + tp.getA() + " of type " + tp.getB().getType().getKey() +
                        " but it's occupied by a component of another type " + this.getSubComponent(tp.getA()).map(c -> c.getType().getKey()).orElseThrow());
                // Otherwise deserialization has been handled in the map() body above, and needs no more action
            });
        MinecraftForge.EVENT_BUS.post(new EntityComponentManagerDeserializeEvent.After(this.getEntity(), nbt));
    }

    private CompoundTag serializeComponent(String name, IEntityComponent<? extends Entity> component) {
        try {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("type", component.getType().getKey().toString());
            nbt.put("data", Optional.ofNullable(component.serializeNBT()).orElseGet(CompoundTag::new));
            CompoundTag subcomponents = new CompoundTag();
            component.getSubComponents().entrySet().stream()
                .filter(entry -> entry.getValue().shouldSerialize())
                .forEach(entry -> subcomponents.put(name, serializeComponent(entry.getKey(), entry.getValue())));
            nbt.put("subcomponents", subcomponents);
            return nbt;
        } catch (RuntimeException e) {
            LogUtils.getLogger().error(e.getMessage());
            LogUtils.getLogger().error("Component " + name + " serialization failed. Discarded.");
            return new CompoundTag();
        }
    }

    /**
     * Deserialize a given component and all downstream components, or rebuild from type if it's absent.
     */
    /* The format is: {
        "type": {component_type},
        "data": {NBT which is handled in the component's deserialize() method},
        "subcomponents": {
            "name1": {subcomponent data in the same format},
            "name2": ...,
            "name3": ...
        }
     }
     */
    @Nullable
    private <T extends IEntityComponent<? extends Entity>> T deserializeOrRebuildComponent(Entity e, @Nullable T component, CompoundTag nbt) {
        try {
            // Name is read in the parent
            EntityComponentType<? extends Entity, ? extends IEntityComponent<? extends Entity>> type =
                NFURegistries.ENTITY_COMPONENT_TYPES.getValue(new ResourceLocation(nbt.getString("type")));
            if (type == null) {
                // If missing factory, cut this branch
                return null;
            }
            if (component != null && !component.getType().equals(type)) {
                throw new IllegalStateException("Component type conflict: deserializing " + type.getKey() + " + to path " +
                    component.getPathFromRoot() + ", but the path is occupied by a component of " + component.getType().getKey());
            }
            T res = component == null ? (T)type.createUnsafe(this.getEntity()) : component;
            res.setSerialize(true); // Components loaded from NBT should be always serialized
            // Deserialize this component
            res.deserializeNBT(nbt.getCompound("data"));
            // Recursively deserialize sub-components
            Tag subcomponentTagRaw = nbt.get("subcomponents");
            if (subcomponentTagRaw instanceof CompoundTag subcomponentTag) {
                subcomponentTag.getAllKeys().stream().filter(k -> !subcomponentTag.getCompound(k).isEmpty())
                    .map(k -> new AbstractMap.SimpleEntry<>(k, deserializeOrRebuildComponent(e, res.getSubComponent(k).orElse(null), subcomponentTag.getCompound(k))))
                    .forEach(entry -> {
                        // If the component is absent, add it
                        if (res.getSubComponent(entry.getKey()).isEmpty())
                            res.addSubComponent(entry.getKey(), entry.getValue());
                            // this means occupied by a component of wrong type, and shouldn't happen
                        else if (res.getSubComponent(entry.getKey(), entry.getValue().getType()).isEmpty())
                            throw new IllegalStateException("Component type conflict: deserializing subcomponent /" + entry.getKey() + " of type " + entry.getValue().getType().getKey() +
                                " but it's occupied by a component of another type " + res.getSubComponent(entry.getKey()).map(c -> c.getType().getKey()).orElseThrow());
                        // Otherwise deserialization has been handled in the map() body above, and needs no more action
                    });
            }
            // Legacy format, TODO remove in 0.x.34
            if (subcomponentTagRaw instanceof ListTag subcomponentTag) {
                subcomponentTag.stream().filter(tag -> tag instanceof CompoundTag)
                    .map(tag -> (CompoundTag) tag)
                    .filter(tag -> tag.contains("name", Tag.TAG_STRING))    // Missing name = skip
                    // Load subcomponents, deserialize or rebuild if absent
                    .map(t -> new Tuple2<>(t.getString("name"), deserializeOrRebuildComponent(e, res.getSubComponent(t.getString("name")).orElse(null), t)))
                    .forEach(tp -> {
                        // If the component is absent, add it
                        if (res.getSubComponent(tp.getA()).isEmpty())
                            res.addSubComponent(tp.getA(), tp.getB());
                            // this means occupied by a component of wrong type, and shouldn't happen
                        else if (res.getSubComponent(tp.getA(), tp.getB().getType()).isEmpty())
                            throw new IllegalStateException("Component type conflict: deserializing subcomponent /" + tp.getA() + " of type " + tp.getB().getType().getKey() +
                                " but it's occupied by a component of another type " + res.getSubComponent(tp.getA()).map(c -> c.getType().getKey()).orElseThrow());
                        // Otherwise deserialization has been handled in the map() body above, and needs no more action
                    });
            }

            return res;
        } catch (RuntimeException ex) {
            LogUtils.getLogger().error(ex.getMessage());
            return null;
        }
    }

    @Override
    public CEntityTickingCapability.TickingSide getTickingSide() {
        return CEntityTickingCapability.TickingSide.BOTH;
    }

    private static record RequiredComponentInfo(
        IEntityComponent<? extends Entity> requiredBy,
        String relPath,
        EntityComponentType<? extends Entity, ? extends IEntityComponent<? extends Entity>> type){}}