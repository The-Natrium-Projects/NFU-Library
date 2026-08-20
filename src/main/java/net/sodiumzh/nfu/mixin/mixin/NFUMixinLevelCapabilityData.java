package net.sodiumzh.nfu.mixin.mixin;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.common.MinecraftForge;
import net.neoforged.common.util.INBTSerializable;
import net.neoforged.common.util.LevelCapabilityData;
import net.sodiumzh.nfu.mixin.NFUMixin;
import net.sodiumzh.nfu.mixin.event.level.LevelCapabilityDataLoadEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelCapabilityData.class)
public class NFUMixinLevelCapabilityData implements NFUMixin<LevelCapabilityData>
{
	@Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraftforge/common/util/INBTSerializable;)Lnet/minecraftforge/common/util/LevelCapabilityData;",
			at = @At("HEAD"), remap = false)
	private static void modifyData(CompoundTag nbt, INBTSerializable<CompoundTag> serializable, CallbackInfoReturnable<LevelCapabilityData> callback)
	{
		MinecraftForge.EVENT_BUS.post(new LevelCapabilityDataLoadEvent(nbt));
	}

}