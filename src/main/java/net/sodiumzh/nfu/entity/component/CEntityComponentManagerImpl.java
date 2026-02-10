package net.sodiumzh.nfu.entity.component;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.capability.CEntityTickingCapability;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.util.NFUMiscStatics;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Default implementation for CEntityComponentManager.
 * Acts as root node and manages child components. Root's parent is always null.
 * Handles ticking the whole component tree: each component is ticked exactly once, in parent-before-children order.
 */
final class CEntityComponentManagerImpl extends EntityComponentBase<Entity> implements CEntityComponentManager {

    public CEntityComponentManagerImpl(Entity entity) {
        super(entity);
    }

    @Override
    public Optional<IEntityComponent<? super Entity>> getParent() {
        // As the root, parent is always null.
        return Optional.empty();
    }

    @Override
    public void attachTo(@Nullable IEntityComponent<? super Entity> parent, String name) {
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
        // Checking required components may be costly, so do it only each 5s
        if (this.getEntity().tickCount % 100 == 50) {
            List<RequiredComponentInfo> missing = this.checkRequiredComponents();
            if (!missing.isEmpty()) {
                for (RequiredComponentInfo info: missing) {
                    LogUtils.getLogger().error("Missing required component or type mismatch: \""
                    + info.relPath() + "\", required by \"" + info.requiredBy().getPathFromRoot()
                    + "\", type: \"" + info.type().getKey().toString() + "\"");
                }
                throw new IllegalStateException("Missing required component or type mismatch. See log above for details.");
            }
        }
        this.getAllDownstreamComponents().forEach(IEntityComponent::tick);
    }

    @Override
    public EntityComponentType<Entity, CEntityComponentManager> getType() {
        return EntityComponentTypes.ROOT.get();
    }


    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        this.getSubComponents().forEach((key, value) -> nbt.put(key, serializeComponent(key, value)));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.clearSubComponents();
        nbt.getAllKeys().stream().map(nbt::getCompound).filter(t -> !t.isEmpty())
            .map(t -> new Tuple2<>(t.getString("name"), deserializeComponent(this.getEntity(), t)))
            .filter(tp -> tp.getB() != null)
            .forEach(tp -> this.addSubComponent(tp.getA(), tp.getB()));
        // Sometimes required components are not correctly serialized. Reconstruct if absent.
        this.getAllRequired().entrySet().stream()
            .filter(entry -> this.getSubComponentByPath(entry.getKey(), entry.getValue()).isEmpty())
            .forEach(entry -> this.addSubComponentByPath(entry.getKey(), entry.getValue().createUnsafe(this.getEntity())));
    }

    private CompoundTag serializeComponent(String name, IEntityComponent<? extends Entity> component) {
        try {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("name", name);
            nbt.putString("type", component.getType().getKey().toString());
            nbt.put("data", component.serializeNBT());
            ListTag subcomponents = new ListTag();
            component.getSubComponents().forEach((key, value) -> subcomponents.add(serializeComponent(key, value)));
            nbt.put("subcomponents", subcomponents);
            return nbt;
        } catch (RuntimeException e) {
            LogUtils.getLogger().error(e.getMessage());
            LogUtils.getLogger().error("Component " + name + " serialization failed. Discarded.");
            return new CompoundTag();
        }
    }

    @Nullable
    private IEntityComponent<? extends Entity> deserializeComponent(Entity e, CompoundTag nbt) {
        try {
            // Name is read in the parent
            EntityComponentType<? extends Entity, ? extends IEntityComponent<? extends Entity>> type =
                NFURegistries.ENTITY_COMPONENT_TYPES.getValue(new ResourceLocation(nbt.getString("type")));
            if (type == null) {
                // If missing factory, cut this branch
                return null;
            }
            IEntityComponent<Entity> component = (IEntityComponent<Entity>) type.createUnsafe(e);
            component.deserializeNBT(nbt.getCompound("data"));
            ListTag subcomponentTag = nbt.getList("subcomponents", Tag.TAG_COMPOUND);
            subcomponentTag.stream().map(tag -> NFUMiscStatics.cast(tag, CompoundTag.class))
                .filter(Objects::nonNull)
                .map(t -> new Tuple2<>(t.getString("name"), deserializeComponent(e, t)))
                .filter(tp -> tp.getB() != null)
                .forEach(tp -> component.addSubComponent(tp.getA(), tp.getB()));
            return component;
        } catch (RuntimeException ex) {
            LogUtils.getLogger().error(ex.getMessage());
            return null;
        }
    }

    private List<RequiredComponentInfo> checkRequiredComponents() {
        List<RequiredComponentInfo> res = new ArrayList<>(getMissingRequiredComponents(this));
        this.getAllDownstreamComponents().stream().map(CEntityComponentManagerImpl::getMissingRequiredComponents)
            .forEach(res::addAll);
        return res;
    }

    private static List<RequiredComponentInfo> getMissingRequiredComponents(IEntityComponent<? extends Entity> component) {
        return component.getAllRequired().entrySet().stream().filter(entry -> {
            @Nullable IEntityComponent<? extends Entity> c = component.getSubComponentByPath(entry.getKey()).orElse(null);
            return c == null || !c.getType().equals(entry.getValue());
        }).map(entry -> new RequiredComponentInfo(component, entry.getKey(), entry.getValue())).toList();
    }

    @Override
    public CEntityTickingCapability.TickingSide getTickingSide() {
        return CEntityTickingCapability.TickingSide.BOTH;
    }

    private static record RequiredComponentInfo(
        IEntityComponent<? extends Entity> requiredBy,
        String relPath,
        EntityComponentType<? extends Entity, ? extends IEntityComponent<? extends Entity>> type){}
}