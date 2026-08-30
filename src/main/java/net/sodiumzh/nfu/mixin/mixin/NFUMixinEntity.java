package net.sodiumzh.nfu.mixin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.mixin.NFUMixin;
import net.sodiumzh.nfu.mixin.NFUMixinHooks;
import net.sodiumzh.nfu.mixin.event.entity.*;
import net.sodiumzh.nfu.util.NFUEntityStatics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class NFUMixinEntity implements NFUMixin<Entity> {

	@Shadow public abstract InteractionResult interact(Player pPlayer, InteractionHand pHand);

	@Inject(method = "<init>", at = @At("TAIL"))
	private void nfu_onEntityConstructor(CallbackInfo ci) {
		MinecraftForge.EVENT_BUS.post(new EntityFinishConstructionEvent(caller()));
	}

	@Inject(at = @At("HEAD"), method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", cancellable = true)
	private void nfu_hurt(DamageSource src, float amount, CallbackInfoReturnable<Boolean> callback)
	{
		if (NFUMixinHooks.onNonLivingEntityHurt(caller(), src, amount))
			callback.setReturnValue(false);
	}

	@Inject(at = @At("HEAD"), method = "tick()V")
	private void nfu_startBaseTick(CallbackInfo callback)
	{
		MinecraftForge.EVENT_BUS.post(new EntityStartBaseTickEvent(caller()));
	}

	@Inject(at = @At("TAIL"), method = "tick()V")
	private void nfu_finishBaseTick(CallbackInfo callback)
	{
		MinecraftForge.EVENT_BUS.post(new EntityFinishBaseTickEvent(caller()));
	}

	@Inject(at = @At("HEAD"), method = "load(Lnet/minecraft/nbt/CompoundTag;)V")
	private void nfu_beforeLoad(CompoundTag nbt, CallbackInfo callback)
	{
		MinecraftForge.EVENT_BUS.post(new EntityLoadEvent(caller(), nbt));
	}

	@WrapOperation(method = "load(Lnet/minecraft/nbt/CompoundTag;)V",
		at = @At(value = "INVOKE", target = "net/minecraft/world/entity/Entity.deserializeCaps (Lnet/minecraft/nbt/CompoundTag;)V", remap = false))
	private void nfu_onDeserializeCaps(Entity instance, CompoundTag compoundTag, Operation<Void> original) {
		MinecraftForge.EVENT_BUS.post(new EntityCapabilityLoadEvent(instance));
		original.call(instance, compoundTag);
		MinecraftForge.EVENT_BUS.post(new EntityCapabilityFinishLoadingEvent(instance));
	}

	@Inject(at = @At("TAIL"), method = "load(Lnet/minecraft/nbt/CompoundTag;)V")
	private void nfu_afterLoad(CompoundTag nbt, CallbackInfo callback)
	{
		MinecraftForge.EVENT_BUS.post(new EntityFinalizeLoadingEvent(caller(), nbt));
	}

	@Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)V", at =
			@At(value = "INVOKE", target = "net/minecraft/CrashReport.forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"),
	cancellable = true)
	private void nfu_loadFailed(CompoundTag pCompound, CallbackInfo ci,
							@Local(ordinal = 0) Throwable throwable,
							@Local(ordinal = 0, argsOnly = true) CompoundTag nbt)
	{
		EntityLoadFailedEvent event = new EntityLoadFailedEvent(caller(), throwable, nbt);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.isShouldIgnore()) {
			MinecraftForge.EVENT_BUS.post(new EntityFinalizeLoadingEvent(caller(), nbt));
			ci.cancel();
		}
	}

	@Inject(method = "discard()V", at = @At("HEAD"))
	private void nfu_onDiscard(CallbackInfo ci) {
		MinecraftForge.EVENT_BUS.post(new EntityDiscardEvent(caller()));
	}

	@WrapOperation(method = "rideTick()V",
		at = @At(value = "INVOKE", target = "net/minecraft/world/entity/Entity.tick()V"))
	private void nfu_onRideTick(Entity instance, Operation<Void> original) {
		NFUEntityStatics.notifyEntityTickStart(instance);
		MinecraftForge.EVENT_BUS.post(new EntityStartTickEvent(instance));
		original.call(instance);
		MinecraftForge.EVENT_BUS.post(new EntityFinishTickEvent(instance));
		NFUEntityStatics.notifyEntityTickEnd(instance);
	}

	@Inject(method = "makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V",
		at = @At("HEAD"), cancellable = true)
	private void nfu_onStuckInBlock(BlockState pState, Vec3 pMotionMultiplier, CallbackInfo ci,
								@Local(argsOnly = true) LocalRef<Vec3> multiplierRef)
	{
		var event = new EntityStuckInBlockEvent(caller(), pState, pMotionMultiplier);
		if (MinecraftForge.EVENT_BUS.post(event)) {
			ci.cancel();
			return;
		}
		if (!event.getMotionMultiplier().equals(event.getOriginalMotionMultiplier())) {
			multiplierRef.set(event.getMotionMultiplier());
		}
	}
}
