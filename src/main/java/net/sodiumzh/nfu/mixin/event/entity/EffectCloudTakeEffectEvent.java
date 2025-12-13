package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.sodiumzh.nfu.event.NFUEntityEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Posted when an {@link AreaEffectCloud} is trying to add effect(s) to a living entity.
 * If multiple entities are affected, the event will be posted once for each affected entity.
 * You can edit the effect list (accessed by {@code getApplyingEffects}) to modify effects to add.
 * Note that the amount of effects also impacts the cloud size shrinkage proportionally.
 */
@Cancelable
public class EffectCloudTakeEffectEvent extends NFUEntityEvent<AreaEffectCloud> {

    private final List<MobEffectInstance> applyingEffects = new ArrayList<>();
    private final LivingEntity affectedEntity;

    public EffectCloudTakeEffectEvent(AreaEffectCloud entity, LivingEntity affectedEntity, List<MobEffectInstance> applyingEffects) {
        super(entity);
        this.applyingEffects.addAll(applyingEffects);
        this.affectedEntity = affectedEntity;
    }

    public List<MobEffectInstance> getApplyingEffects() {
        return applyingEffects;
    }

    public LivingEntity getAffectedEntity() {
        return affectedEntity;
    }
}
