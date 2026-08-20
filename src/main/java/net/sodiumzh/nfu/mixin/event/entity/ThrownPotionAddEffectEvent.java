package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.neoforged.eventbus.api.Cancelable;
import net.sodiumzh.nfu.event.NFUEntityEvent;

import java.util.List;

@Cancelable
public class ThrownPotionAddEffectEvent extends NFUEntityEvent<ThrownPotion> {

    private List<MobEffectInstance> effects;
    private LivingEntity target;

    public ThrownPotionAddEffectEvent(ThrownPotion entity, LivingEntity target, List<MobEffectInstance> effects) {
        super(entity);
        this.target = target;
        this.effects = effects;
    }

    public List<MobEffectInstance> getEffects() {
        return effects;
    }

    public void setEffects(List<MobEffectInstance> effects) {
        this.effects = effects;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }
}
