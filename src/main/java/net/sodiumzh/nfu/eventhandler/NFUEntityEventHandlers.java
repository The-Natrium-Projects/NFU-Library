package net.sodiumzh.nfu.eventhandler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.mixin.event.entity.EntityFinalizeLoadingEvent;
import net.sodiumzh.nfu.mixin.event.entity.EntityFinishConstructionEvent;
import net.sodiumzh.nfu.mixin.event.entity.ItemEntityHurtEvent;
import net.sodiumzh.nfu.registry.NFUTags;

@Mod.EventBusSubscriber(modid = NFULibrary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NFUEntityEventHandlers {

    @SubscribeEvent
    public static void onItemEntityHurt(ItemEntityHurtEvent event) {
        // Handle tag "nfulib:explosion_not_breaking_items"
        if (event.damageSource.is(DamageTypes.EXPLOSION) || event.damageSource.is(DamageTypes.PLAYER_EXPLOSION)) {
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
    public static void onEntityFinalizeLoading(EntityFinalizeLoadingEvent event) {
        // Record entity type into forge data, so that we can read the type from an NBT from saveWithoutId
        // If this data is absent in the loading nbt, it may be wiped after loading, so add it again
        event.getEntity().getPersistentData().putString("NFU_EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString());
    }

    @SubscribeEvent
    public static void onEntityFinalizeConstrction(EntityFinishConstructionEvent event) {
        // Record entity type into forge data, so that we can read the type from an NBT from saveWithoutId
        event.getEntity().getPersistentData().putString("NFU_EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString());
    }


}
