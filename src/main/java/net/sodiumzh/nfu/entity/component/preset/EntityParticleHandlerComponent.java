package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.sodiumzh.nfu.annotation.NotYetImplemented;
import net.sodiumzh.nfu.entity.component.EntityComponentBase;

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
