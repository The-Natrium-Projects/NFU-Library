package net.sodiumzh.nfu.mixin.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.sodiumzh.nfu.exception.EntityLoadingFailureException;
import net.sodiumzh.nfu.mixin.NFUMixin;
import net.sodiumzh.nfu.registry.NFUConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PersistentEntitySectionManager.class)
public class NFUPersistentEntitySectionManagerMixin implements NFUMixin<NFUPersistentEntitySectionManagerMixin> {

    @Inject(method = "lambda$requestChunkLoad$8(Lnet/minecraft/world/level/ChunkPos;Ljava/lang/Throwable;)Ljava/lang/Void;",
    at = @At("HEAD"))
    private static void throwEntityLoadFailure(ChunkPos chunkpos, Throwable throwable, CallbackInfoReturnable<Void> cir) throws Throwable {
        if (NFUConfigs.CACHED_CRASHES_ON_ENTITY_LOAD_FAILED && throwable instanceof EntityLoadingFailureException)
            throw throwable;
    }
}
