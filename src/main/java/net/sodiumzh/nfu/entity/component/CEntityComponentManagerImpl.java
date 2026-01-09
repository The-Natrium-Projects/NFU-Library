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
final class CEntityComponentManagerImpl extends EntityComponentBase implements CEntityComponentManager {

    public CEntityComponentManagerImpl(Entity entity) {
        super(entity);
    }

    @Override
    public Optional<IEntityComponent> getParent() {
        // As the root, parent is always null.
        return Optional.empty();
    }

    @Override
    public void attachTo(@Nullable IEntityComponent parent, String name) {
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
    public EntityComponentType<?> getType() {
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
    }

    private CompoundTag serializeComponent(String name, IEntityComponent component) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", name);
        nbt.putString("type", component.getType().getKey().toString());
        nbt.put("data", component.serializeNBT());
        ListTag subcomponents = new ListTag();
        component.getSubComponents().forEach((key, value) -> subcomponents.add(serializeComponent(key, value)));
        nbt.put("subcomponents", subcomponents);
        return nbt;
    }

    @Nullable
    private IEntityComponent deserializeComponent(Entity e, CompoundTag nbt) {
        // Name is read in the parent
        EntityComponentType<?> type = NFURegistries.ENTITY_COMPONENT_TYPES.getValue(new ResourceLocation(nbt.getString("type")));
        if (type == null) {
            // If missing factory, cut this branch
            return null;
        }
        IEntityComponent component = type.factory().create(e);
        component.deserializeNBT(nbt.getCompound("data"));
        ListTag subcomponentTag = nbt.getList("subcomponents", Tag.TAG_COMPOUND);
        subcomponentTag.stream().map(tag -> NFUMiscStatics.cast(tag, CompoundTag.class))
            .filter(Objects::nonNull)
            .map(t -> new Tuple2<>(t.getString("name"), deserializeComponent(e, t)))
            .filter(tp -> tp.getB() != null)
            .forEach(tp -> component.addSubComponent(tp.getA(), tp.getB()));
        return component;
    }

    private List<RequiredComponentInfo> checkRequiredComponents() {
        List<RequiredComponentInfo> res = new ArrayList<>(getMissingRequiredComponents(this));
        this.getAllDownstreamComponents().stream().map(CEntityComponentManagerImpl::getMissingRequiredComponents)
            .forEach(res::addAll);
        return res;
    }

    private static List<RequiredComponentInfo> getMissingRequiredComponents(IEntityComponent component) {
        return component.getAllRequired().entrySet().stream().filter(entry -> {
            @Nullable IEntityComponent c = component.getSubComponentByPath(entry.getKey()).orElse(null);
            return c == null || !c.getType().equals(entry.getValue());
        }).map(entry -> new RequiredComponentInfo(component, entry.getKey(), entry.getValue())).toList();
    }

    @Override
    public CEntityTickingCapability.TickingSide getTickingSide() {
        return CEntityTickingCapability.TickingSide.BOTH;
    }

    private static record RequiredComponentInfo(IEntityComponent requiredBy, String relPath, EntityComponentType<?> type){}
}