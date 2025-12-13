package net.sodiumzh.nfu.eventhandler;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
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

    public static void onLivingTick(LivingEvent.LivingUpdateEvent event) {
        if (NFUConfigs.CACHED_ENABLES_FLYING_SPEED_SCALING_FIX) {
            Optional.ofNullable(event.getEntityLiving().getAttribute(Attributes.FLYING_SPEED))
                .ifPresent(i -> event.getEntityLiving().flyingSpeed = (float) i.getValue() * 0.05f);
        }
    }


}
