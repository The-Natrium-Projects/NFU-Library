package net.sodiumzh.nfu.entity.component;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.entity.component.preset.IEntityComponentAccess;
import net.sodiumzh.nfu.network.AvailableSide;
import net.sodiumzh.nfu.object.HierarchyPath;
import net.sodiumzh.nfu.object.Validatable;
import net.sodiumzh.nfu.registry.NFUConfigs;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.util.NFUDebugStatics;
import net.sodiumzh.nfu.util.NFUNBTStatics;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Default implementation for CEntityComponentManager.
 * Acts as root node and manages child components. Root's parent is always null.
 * Handles ticking the whole component tree: each component is ticked exactly once, in parent-before-children order.
 */
final class CEntityComponentManagerImpl extends EntityComponentBase<Entity> implements CEntityComponentManager {

    private static ThreadLocal<Long> CONSTRUCT_COUNT = ThreadLocal.withInitial(() -> 0L);

    private boolean constructionDone = false;
    private final Validatable<Map<HierarchyPath, IEntityComponent<?>>> preConstructed = new Validatable<>(new HashMap<>());    // Valid only in construction. Invalidated after construction.
    // Count tick/serialization errors, and throws if too many errors are encountered
    // Excessive error count indicates tickly errors which are not tolerated
    private int errorCount = 0;

    CEntityComponentManagerImpl(Entity entity) {
        super(entity);
        this.construct();
    }

    private void construct() {
        // Running construction in EntityComponentAPI#getComponentManager will cause infinite recursion.
        // Backtrace check each 100 times to catch infinite recursion
        // This check is disabled in runtime as it could be costly
        long count = CONSTRUCT_COUNT.get() + 1;
        CONSTRUCT_COUNT.set(count);
        if (count % 100 == 0) {
            boolean infRec = StackWalker.getInstance().walk(stream -> stream.limit(100)
                .anyMatch(stackFrame -> stackFrame.getClassName().equals(EntityComponentAPI.class.getName())
                    && stackFrame.getMethodName().equals("getComponentManager")));
            if (infRec) {
                throw new IllegalCallerException("Illegal call EntityComponentAPI#getComponentManager in component manager construction phase. "
                    + "Note that EntityComponentSetupEvent and EntityComponentFinalizeSetupEvent are posted during construction and should not call EntityComponentAPI#getComponentManager. "
                    + "Use event.getComponentManager() to access the manager in the event listeners instead.");
            }
        }
        // Enter construction phase, collect component construction info by event
        preConstructed.validate();
        EntityComponentSetupEvent initEvent = new EntityComponentSetupEvent(this.getEntity(), this);
        initEvent.sharePreConstructedMapFrom(this.preConstructed.get());  // Share the map reference to event, so that it can be filled during event post
        MinecraftForge.EVENT_BUS.post(initEvent);
        // First pre-construct needed components and collect the result
        initEvent.sharePreConstructedMapFrom(this.preConstructed.get());
        initEvent.preConstruct();
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
        if (NFUConfigs.CACHED_ENTITY_COMPONENT_HIERARCHY_CHECK) {
            this.checkHierarchyOfAllComponents();
        }
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
        } else return Optional.empty();
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
        this.getDownstreamComponents().stream()
            .filter(c -> c.tickingSide().isCorrectSide(this.getEntity()))
            .filter(IEntityComponent::shouldTick)
            .map(e -> Tuple2.of(e.pathDepth(), e))
            .sorted(Comparator.comparingInt(Tuple2::getA))
            .forEach(e -> {
                try {
                    e.getB().tick();
                } catch (Exception ex) {
                    LogUtils.getLogger().error("NFU: Exception thrown on entity component tick");
                    LogUtils.getLogger().error("Entity: " + this.getEntity().getName().getString() + "; Component path: \"" + e.getB().getPathFromRoot() + "\"");
                    throw ex;
                }
            });
        // Check hierarchy if configured each 10s
        if (NFUConfigs.CACHED_ENTITY_COMPONENT_HIERARCHY_CHECK && this.getEntity().tickCount % 200 == 0) {
            this.checkHierarchyOfAllComponents();
        }
    }

    @Override
    public void joinLevel() {
        this.getDownstreamComponents().stream()
            .map(e -> Tuple2.of(e.pathDepth(), e))
            .sorted(Comparator.comparingInt(Tuple2::getA))
            .forEach(e -> e.getB().joinLevel());
    }

    private void checkHierarchyOfAllComponents() {
        this.getAllPathsAndDownstreamComponents().entrySet().stream()
            .peek(entry -> {
                if (entry.getKey().length() >= 64)
                    throw new IllegalStateException("Too deep path (>=64). Cyclic hierarchy path? Path: " + entry.getKey().toString());
            })
            .filter(e -> e.getValue() instanceof EntityComponentBase<? extends Entity>)
            // Deeper first here, so that we can catch
            .sorted(Comparator.comparingInt((Map.Entry<HierarchyPath, IEntityComponent<?>> e) -> e.getKey().length()))
            .map(entry -> (EntityComponentBase<?>) (entry.getValue()))
            .forEach(EntityComponentBase::checkHierarchy);
    }


    @Override
    public EntityComponentType<Entity, CEntityComponentManager> getType() {
        return EntityComponentTypes.ROOT.get();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        MinecraftForge.EVENT_BUS.post(new EntityComponentManagerSerializeEvent.Before(this.getEntity(), nbt));
        this.getAllPathsAndDownstreamComponents().entrySet().stream()
            .filter(entry -> entry.getValue().shouldSerialize())
            .sorted(Comparator.comparingInt(entry -> entry.getKey().length()))
            .forEach(entry -> {
                try {
                    CompoundTag nbt1 = new CompoundTag();
                    nbt1.putString("type", entry.getValue().getType().getKey().toString());
                    nbt1.putBoolean("rebuild", entry.getValue().shouldRebuildOnDeserialization());
                    nbt1.put("data", Optional.ofNullable(entry.getValue().serializeNBT()).orElse(new CompoundTag()));
                    nbt.put(entry.getKey().toLiteral(), nbt1);
                } catch (Exception ex) {
                    LogUtils.getLogger().error("NFU: Exception thrown on entity component save");
                    LogUtils.getLogger().error("Entity: " + this.getEntity().getName().getString() + "; Component path: \"" + entry.getValue().getPathFromRoot() + "\"");
                    throw ex;
                }
            });



        /*this.getSubComponents().entrySet().stream().filter(entry -> entry.getValue().shouldSerialize())
            .forEach(entry -> nbt.put(entry.getKey(), serializeComponent(entry.getKey(), entry.getValue())));*/
        MinecraftForge.EVENT_BUS.post(new EntityComponentManagerSerializeEvent.After(this.getEntity(), nbt));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        MinecraftForge.EVENT_BUS.post(new EntityComponentManagerDeserializeEvent.Before(this.getEntity(), nbt));
        NFUNBTStatics.entrySet(nbt).stream()
            .map(entry -> new AbstractMap.SimpleEntry<>(HierarchyPath.byLiteral(entry.getKey()), (CompoundTag) (entry.getValue())))  // Implicitly assert tag type
            .sorted(Comparator.comparingInt(entry -> entry.getKey().length()))
            .forEach(entry -> {
                try {
                    IEntityComponent<?> component = this.getSubComponentByPath(entry.getKey()).orElse(null);
                    if (component == null && entry.getValue().getBoolean("rebuild")) {
                        var type = NFURegistries.ENTITY_COMPONENT_TYPES.getOptionalValue(new ResourceLocation(entry.getValue().getString("type"))).orElse(null);
                        if (type != null) { // Missing type means invalid entry, ignore
                            component = type.createUnsafe(this.getEntity());
                            this.addSubComponentByPath(entry.getKey(), component);
                        }
                    }
                    if (component != null) {
                        component.deserializeNBT(entry.getValue().getCompound("data"));
                    }
                } catch (Exception ex) {
                    LogUtils.getLogger().error("NFU: Exception thrown on entity component load");
                    LogUtils.getLogger().error("Entity: " + this.getEntity().getName().getString() + "; Component path: \"" + entry.getKey() + "\"");
                    ex.printStackTrace();
                    throw ex;
                }
            });

        MinecraftForge.EVENT_BUS.post(new EntityComponentManagerDeserializeEvent.After(this.getEntity(), nbt));
    }

    @Override
    public AvailableSide tickingSide() {
        return AvailableSide.BOTH;
    }

}