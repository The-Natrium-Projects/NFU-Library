package net.sodiumzh.nfu.mixin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.sodiumzh.nfu.mixin.NFUMixin;
import net.sodiumzh.nfu.registry.NFUTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherBoss.class)
public class NFUWitherBossMixin implements NFUMixin<WitherBoss> {

    @Inject(method = "lambda$static$0(Lnet/minecraft/world/entity/LivingEntity;)Z",
        at = @At("HEAD"), cancellable = true)
    private static void nfu_witherTargetSelector(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof Mob mob
            && mob.getMobType() == MobType.UNDEAD
            && mob.getType().is(NFUTags.WITHER_ATTACKABLE_UNDEAD))
        {
            cir.setReturnValue(target.attackable());
        }
    }

    @WrapOperation(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
    at = @At(value = "INVOKE", target = "net/minecraft/world/damagesource/DamageSource.getEntity()Lnet/minecraft/world/entity/Entity;", ordinal = 1))
    private Entity nfu_bypassUndeadInvulnerability(DamageSource instance, Operation<Entity> original) {
        if (instance.getEntity() instanceof Mob mob
            && mob.getMobType() == MobType.UNDEAD
            && mob.getType().is(NFUTags.WITHER_ATTACKABLE_UNDEAD) )
        {
            return null;
        }
        return original.call(instance);
    }

}
