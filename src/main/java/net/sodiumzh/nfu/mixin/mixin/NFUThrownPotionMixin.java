package net.sodiumzh.nfu.mixin.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.mixin.NFUMixin;
import net.sodiumzh.nfu.mixin.event.entity.ThrownPotionAddEffectEvent;
import net.sodiumzh.nfu.mixin.event.entity.ThrownPotionEffectCloudEvent;
import net.sodiumzh.nfu.util.NFUContainerStatics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ThrownPotion.class)
public class NFUThrownPotionMixin implements NFUMixin<ThrownPotion> {

    @ModifyReceiver(method = "applySplash(Ljava/util/List;Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE",
        target = "java/util/List.iterator()Ljava/util/Iterator;", remap = false, ordinal = 1))
    private List<MobEffectInstance> nfu_PostEffectEvent(
        List<MobEffectInstance> instance, @Local(ordinal = 0) LivingEntity target)
    {
        ThrownPotionAddEffectEvent event = new ThrownPotionAddEffectEvent(this.caller(), target, new ArrayList<>(instance));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return List.of();
        if (NFUContainerStatics.listEquals(event.getEffects(), instance)) return instance;
        else return event.getEffects();
    }

    // Add a way to manipulate whether lingering by NBT, overriding item type
    @ModifyReturnValue(method = "isLingering()Z", at = @At(value = "RETURN"))
    private boolean nfu_LingeringByNBT(boolean original) {
        if (this.caller().getItem().hasTag() && this.caller().getItem().getTag().contains("isLingering", Tag.TAG_ANY_NUMERIC)) {
            return this.caller().getItem().getTag().getBoolean("isLingering");
        }
        return original;
    }

    @Inject(method = "makeAreaOfEffectCloud(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/alchemy/Potion;)V",
        at = @At(value = "INVOKE", target = "net/minecraft/world/entity/projectile/ThrownPotion.getOwner ()Lnet/minecraft/world/entity/Entity;"),
        cancellable = true)
    private void nfu_PostAddCloudEventConstruct(
        ItemStack pStack,
        Potion pPotion,
        CallbackInfo ci,
        @Local(ordinal = 0) LocalRef<AreaEffectCloud> cloudRef)
    {
        ThrownPotionEffectCloudEvent.Construct event = new ThrownPotionEffectCloudEvent.Construct(this.caller(), cloudRef.get(), pStack, pPotion);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled() || event.getCloud() == null)
            ci.cancel();
        else if (!event.getCloud().equals(cloudRef.get()))
            cloudRef.set(event.getCloud());
    }

    @Inject(method = "makeAreaOfEffectCloud(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/alchemy/Potion;)V",
        at = @At(value = "INVOKE", target = "net/minecraft/world/level/Level.addFreshEntity (Lnet/minecraft/world/entity/Entity;)Z",
            ordinal = 1), cancellable = true)
    private void nfu_PostAddCloudEventSpawn(
        ItemStack pStack,
        Potion pPotion,
        CallbackInfo ci,
        @Local(ordinal = 0) LocalRef<AreaEffectCloud> cloudRef)
    {
        ThrownPotionEffectCloudEvent.Spawn event = new ThrownPotionEffectCloudEvent.Spawn(this.caller(), cloudRef.get());
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled() || event.getCloud() == null)
            ci.cancel();
        else if (!event.getCloud().equals(cloudRef.get()))
            cloudRef.set(event.getCloud());
    }

}
