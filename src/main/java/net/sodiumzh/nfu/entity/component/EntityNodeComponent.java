package net.sodiumzh.nfu.entity.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.checkerframework.checker.units.qual.C;

/**
 * A component which serves only as a hierarchy node for subcomponents and does nothing by itself.
 */
public final class EntityNodeComponent extends EntityComponentBase<Entity>{

     // This component is hard-bound with {@link EntityComponentTypes#NODE}. No instantiation elsewhere
    EntityNodeComponent(Entity entity) {
        super(entity);
    }

    @Override
    public void tick() {
    }

    @Override
    public CompoundTag serializeNBT() {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }
}
