package net.sodiumzh.nfu.mixin.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.mixin.NFUMixin;
import net.sodiumzh.nfu.mixin.event.entity.EffectCloudTakeEffectEvent;
import net.sodiumzh.nfu.registry.NFUCapabilities;
import net.sodiumzh.nfu.util.NFUContainerStatics;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(AreaEffectCloud.class)
public class NFUAreaEffectCloudMixin implements NFUMixin<AreaEffectCloud> {

    @Inject(method = "tick()V", at = @At(value = "INVOKE",
        target = "java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", remap = false))
    private void nfu_PostEffectEvent(
        CallbackInfo ci,
        @Local(ordinal = 0) List<MobEffectInstance> effects,
        @Local(ordinal = 0) LivingEntity target)
    {
        EffectCloudTakeEffectEvent event = new EffectCloudTakeEffectEvent(this.caller(), target, new ArrayList<>(effects));
        MinecraftForge.EVENT_BUS.post(event);
        List<MobEffectInstance> actualList = event.isCanceled() ? List.of() : event.getApplyingEffects();
        // Label if not modified, and the original effect will be directly taken in the following mixins,
        // reducing the risk that someone else mixins into this process
        boolean isModified = !NFUContainerStatics.listEquals(actualList, effects);
        // Store effects to transient data
        this.caller().getCapability(NFUCapabilities.CAP_ENTITY_DATA).ifPresent(data -> {
            data.putTransientParameter("NFUMixin_PostEffectEvent_EffectListModified", isModified);
            data.putTransientParameter("NFUMixin_PostEffectEvent_ActualEffectList", actualList);
        });
    }

    @ModifyReceiver(method = "tick()V", at = @At(value = "INVOKE",
        target = "java/util/List.iterator()Ljava/util/Iterator;", ordinal = 2, remap = false))
    private List<MobEffectInstance> nfu_ModifyEffectList(List<MobEffectInstance> instance) {
        // Keep vanilla behavior if not modified, to prevent potential mixin compat issues
        if (!this.caller().getCapability(NFUCapabilities.CAP_ENTITY_DATA).resolve().flatMap(d ->
            d.getTransientParameter("NFUMixin_PostEffectEvent_EffectListModified", Boolean.class))
            .orElse(false)) {
            return instance;
        }
        List<MobEffectInstance> effects = new ArrayList<>();
        this.caller().getCapability(NFUCapabilities.CAP_ENTITY_DATA).ifPresent(data ->
            data.getTransientParameter("NFUMixin_PostEffectEvent_ActualEffectList", List.class).ifPresent(effects::addAll));
        return effects;
    }

    @ModifyExpressionValue(method = "tick()V", at = @At(value = "FIELD",
        target = "net/minecraft/world/entity/AreaEffectCloud.radiusOnUse:F",
        opcode = Opcodes.GETFIELD, ordinal = 1))
    private float nfu_ModifyRadiusConsumption(float original, @Local(ordinal = 0) List<MobEffectInstance> effects)
    {
        // Keep vanilla behavior if not modified, to prevent potential mixin compat issues
        if (!this.caller().getCapability(NFUCapabilities.CAP_ENTITY_DATA).resolve().flatMap(d ->
                d.getTransientParameter("NFUMixin_PostEffectEvent_EffectListModified", Boolean.class))
            .orElse(false)) {
            return original;
        }
        AtomicReference<Integer> actualAppliedEffectCount = new AtomicReference<>(0);
        this.caller().getCapability(NFUCapabilities.CAP_ENTITY_DATA).ifPresent(data ->
            data.getTransientParameter("NFUMixin_PostEffectEvent_ActualEffectList", List.class)
                .map(List::size).ifPresent(actualAppliedEffectCount::set));
        int expectedCount = effects.size();
        return original * (float) (actualAppliedEffectCount.get()) / (float)expectedCount;
    }

    @ModifyExpressionValue(method = "tick()V", at = @At(value = "FIELD",
        target = "net/minecraft/world/entity/AreaEffectCloud.durationOnUse:I",
        opcode = Opcodes.GETFIELD, ordinal = 1))
    private int nfu_ModifyDurationConsumption(int original, @Local(ordinal = 0) List<MobEffectInstance> effects)
    {
        // Keep vanilla behavior if not modified, to prevent potential mixin compat issues
        if (!this.caller().getCapability(NFUCapabilities.CAP_ENTITY_DATA).resolve().flatMap(d ->
                d.getTransientParameter("NFUMixin_PostEffectEvent_EffectListModified", Boolean.class))
            .orElse(false)) {
            return original;
        }
        AtomicReference<Integer> actualAppliedEffectCount = new AtomicReference<>(0);
        this.caller().getCapability(NFUCapabilities.CAP_ENTITY_DATA).ifPresent(data ->
            data.getTransientParameter("NFUMixin_PostEffectEvent_ActualEffectList", List.class)
                .map(List::size).ifPresent(actualAppliedEffectCount::set));
        int expectedCount = effects.size();
        this.caller().getCapability(NFUCapabilities.CAP_ENTITY_DATA).ifPresent(data -> {
            data.removeTransientParameter("NFUMixin_PostEffectEvent_EffectListModified");
            data.removeTransientParameter("NFUMixin_PostEffectEvent_ActualEffectList");
        });
        return Math.round((float) original * (float) (actualAppliedEffectCount.get()) / (float)expectedCount);
    }
}
