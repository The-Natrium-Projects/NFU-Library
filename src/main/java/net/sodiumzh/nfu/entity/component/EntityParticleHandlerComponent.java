package net.sodiumzh.nfu.entity.component;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.sodiumzh.nfu.annotation.NotYetImplemented;
import org.jetbrains.annotations.ApiStatus;

@NotYetImplemented
public abstract class EntityParticleHandlerComponent<E extends Entity> extends EntityComponentBase<E> {

    public EntityParticleHandlerComponent(E entity) {
        super(entity);
    }

    public static record ParticleArgs(
        ParticleOptions type,
        Vec3 pos,
        Vec3 randomRange,
        EntityParticleHandlerComponent.RandomizationType randomizationType
    ) {}

    public static enum RandomizationType {
        UNIFORM,
        GAUSSIAN
    }

}
