package net.sodiumzh.nfu.mixin.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.sodiumzh.nfu.exception.EntityLoadingFailureException;
import net.sodiumzh.nfu.exception.InfiniteRecursionException;
import net.sodiumzh.nfu.mixin.NFUMixin;
import net.sodiumzh.nfu.registry.NFUConfigs;
import net.sodiumzh.nfu.savedata.redirector.SaveDataLocationRedirectorEventListeners;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(EntityType.class)
public class NFUMixinEntityType implements NFUMixin<EntityType<?>>
{

	@Inject(method = "create(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
			at = @At("HEAD"))
	private static void startCreate(CompoundTag nbt, Level level, CallbackInfoReturnable<Optional<Entity>> callback)
	{
		SaveDataLocationRedirectorEventListeners.doPortEntityTypes(nbt);
	}

    @Inject(method = "loadStaticEntity(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
        at = @At(value = "INVOKE", target = "java/util/Optional.empty ()Ljava/util/Optional;"))
    private static void throwsOnLoadStaticEntity(CompoundTag pCompound, Level pLevel, CallbackInfoReturnable<Optional<Entity>> cir,
                                                @Local(ordinal = 0) RuntimeException exception)
    {
        if (NFUConfigs.CACHED_CRASHES_ON_ENTITY_LOAD_FAILED) {
            throw new EntityLoadingFailureException("Crashed for an exception thrown on entity loading. To disable crash, set nfulib config (in nfulib_common.toml) 'crashedOnEntityLoadFailed' to false.", exception);
        }
        if (exception instanceof InfiniteRecursionException) {
            throw new EntityLoadingFailureException(exception);
        }
    }
}
