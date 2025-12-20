package net.sodiumzh.nfu.mixin.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceLocation;
import net.sodiumzh.nfu.mixin.NFUMixin;
import net.sodiumzh.nfu.registry.NFUConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(MappedRegistry.class)
public class NFUMappedRegistryMixin implements NFUMixin<MappedRegistry<?>> {

    /**
     * Implement Bypass Unbound Key config feature.
     */
    @WrapOperation(method = "freeze()Lnet/minecraft/core/Registry;",
        at = @At(
            value = "INVOKE",
            target = "java/util/List.isEmpty()Z",
            ordinal = 0))
    private boolean nfu_bypassUnboundKeyCheck(List<ResourceLocation> instance, Operation<Boolean> original) {
        boolean res = original.call(instance);
        if (!res && NFUConfigs.CACHED_BYPASSES_UNBOUND_KEY_CHECK) {
            LogUtils.getLogger().error("Unbound values in registry " + caller().key() + ": " + instance);
            return true;
        }
        return res;
    }


}
