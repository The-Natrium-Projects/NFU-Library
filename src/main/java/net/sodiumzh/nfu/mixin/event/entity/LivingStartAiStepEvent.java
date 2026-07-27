package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.LivingEntity;
import net.sodiumzh.nfu.event.NFULivingEvent;

public class LivingStartAiStepEvent extends NFULivingEvent<LivingEntity> {

    public LivingStartAiStepEvent(LivingEntity entity) {
        super(entity);
    }

}
