package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.LivingEntity;
import net.sodiumzh.nfu.event.NFULivingEvent;

public class LivingFinishAiStepEvent extends NFULivingEvent<LivingEntity> {
    public LivingFinishAiStepEvent(LivingEntity entity) {
        super(entity);
    }
}
