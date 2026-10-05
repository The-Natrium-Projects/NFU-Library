package net.sodiumzh.nfu.mixin.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.sodiumzh.nfu.mixin.NFUMixin;
import net.sodiumzh.nfu.registry.NFUConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityStorage.class)
public class NFUEntityStorageMixin implements NFUMixin<EntityStorage> {

    @Inject(method = "lambda$storeEntities$1(Lnet/minecraft/nbt/ListTag;Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", target = "org/slf4j/Logger.error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private static void nfu_throwsOnSaveFailure(ListTag listtag, Entity p_156567_, CallbackInfo ci, @Local Exception e) {
        if (NFUConfigs.CACHED_CRASHES_ON_ENTITY_LOAD_FAILS) {
            LogUtils.getLogger().error("Entity " + p_156567_ + " saving failed. To disable crash and remove the wrong entity instead, set config \"crashesOnEntityLoadFails\" in nfulib-common.toml to false.");
            throw new RuntimeException(e);
        }
    }

}
