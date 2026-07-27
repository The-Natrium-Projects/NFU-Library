package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.LivingEntity;
import net.sodiumzh.nfu.event.NFULivingEvent;

/**
 * Invoked at the end of {@link LivingEntity#aiStep()}. This event allows to insert code
 * right after base {@code LivingEntity.aiStep()} call and before any override actions after {@code super.aiStep()}.
 */
public class LivingFinishBaseAiStepEvent extends NFULivingEvent<LivingEntity> {

    public LivingFinishBaseAiStepEvent(LivingEntity entity) {
        super(entity);
    }

}
