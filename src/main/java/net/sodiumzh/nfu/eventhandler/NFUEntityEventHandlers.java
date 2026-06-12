package net.sodiumzh.nfu.eventhandler;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.mixin.event.entity.EntityFinalizeLoadingEvent;
import net.sodiumzh.nfu.mixin.event.entity.EntityFinishConstructionEvent;
import net.sodiumzh.nfu.mixin.event.entity.ItemEntityHurtEvent;
import net.sodiumzh.nfu.registry.NFUConfigs;
import net.sodiumzh.nfu.registry.NFUTags;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = NFULibrary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NFUEntityEventHandlers {

    @SubscribeEvent
    public static void onItemEntityHurt(ItemEntityHurtEvent event) {
        // Handle tag "nfulib:explosion_not_breaking_items"
        if (event.damageSource.isExplosion()) {
            if (event.damageSource.getEntity() != null
                && event.damageSource.getEntity().getType().is(NFUTags.EXPLOSION_NOT_BREAKING_ITEMS)) {
                event.setCanceled(true);
            } else if (event.damageSource.getDirectEntity() != null
                && event.damageSource.getDirectEntity().getType().is(NFUTags.EXPLOSION_NOT_BREAKING_ITEMS)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (NFUConfigs.CACHED_ENABLES_FLYING_SPEED_SCALING_FIX) {
            Optional.ofNullable(event.getEntity().getAttribute(Attributes.FLYING_SPEED))
                .ifPresent(i -> event.getEntity().flyingSpeed = (float) i.getValue() * 0.05f);
        }
    }

    @SubscribeEvent
    public static void onEntityFinalizeLoading(EntityFinalizeLoadingEvent event) {
        // Record entity type into forge data, so that we can read the type from an NBT from saveWithoutId
        // If this data is absent in the loading nbt, it may be wiped after loading, so add it again
        event.getEntity().getPersistentData().putString("NFU_EntityType", ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString());
    }

    @SubscribeEvent
    public static void onEntityFinalizeConstrction(EntityFinishConstructionEvent event) {
        // Record entity type into forge data, so that we can read the type from an NBT from saveWithoutId
        event.getEntity().getPersistentData().putString("NFU_EntityType", ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString());
    }


}
