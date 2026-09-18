package net.sodiumzh.nfu.mixin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.mixin.NFUMixin;
import net.sodiumzh.nfu.mixin.event.entity.EntityFinishTickEvent;
import net.sodiumzh.nfu.mixin.event.entity.EntityStartTickEvent;
import net.sodiumzh.nfu.util.NFUEntityStatics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class NFUMixinServerLevel implements NFUMixin<ServerLevel> {

    @Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", target = "net/minecraft/world/entity/Entity.tick()V"))
    private void nfu_beforeTick(Entity instance, CallbackInfo ci) {
        NFUEntityStatics.notifyEntityTickStart(instance);
        MinecraftForge.EVENT_BUS.post(new EntityStartTickEvent(instance));
    }

    @Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", target = "net/minecraft/world/entity/Entity.tick()V", shift = At.Shift.AFTER))
    private void nfu_afterTick(Entity instance, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new EntityFinishTickEvent(instance));
        NFUEntityStatics.notifyEntityTickEnd();
    }

}
