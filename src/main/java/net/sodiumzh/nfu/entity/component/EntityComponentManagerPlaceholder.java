package net.sodiumzh.nfu.entity.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.object.HierarchyPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Transient placeholder when access of component manager is missing.
 */
public class EntityComponentManagerPlaceholder extends EntityComponentBase<Entity> implements CEntityComponentManager {

    public EntityComponentManagerPlaceholder(Entity entity) {
        super(entity);
    }

    @Override
    public EntityComponentType<Entity, CEntityComponentManager> getType() {
        return EntityComponentTypes.ROOT.get();
    }

    @Override
    public void tick() {

    }

    @Override
    public @Nullable CompoundTag serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

    }
}
