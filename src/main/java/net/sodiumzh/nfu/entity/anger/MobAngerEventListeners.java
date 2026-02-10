package net.sodiumzh.nfu.entity.anger;

import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.entity.component.EntityComponentAPI;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = NFULibrary.MOD_ID)
public class MobAngerEventListeners {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTick(LivingEvent.LivingTickEvent event)
    {
        // TODO: remove legacy anger handler cap, use component instead
        if (!event.isCanceled() && event.getEntity() instanceof Mob mob && mob.getTarget() != null) {
            for (var cap : CMobAngerHandler.ALL_HANDLERS) {
                event.getEntity().getCapability(cap).ifPresent(c -> c.setAngryAt(mob.getTarget(), MobAngerReason.TARGETING.get()));
            }
            EntityComponentAPI.getComponentManager(event.getEntity()).getAllDownstreamComponents().stream()
                .filter(c -> c instanceof MobAngerHandlerComponent).map(c -> (MobAngerHandlerComponent)c)
                .forEach(c -> c.setAngryAt(mob.getTarget(), MobAngerReason.TARGETING.get()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!event.isCanceled()
                && event.getSource().getEntity() != null
                && event.getSource().getEntity() instanceof LivingEntity src) {
            // TODO: remove legacy anger handler cap, use component instead
            for (var cap : CMobAngerHandler.ALL_HANDLERS) {
                if (event.getSource() instanceof EntityDamageSource eds && eds.isThorns())
                {
                    event.getEntity().getCapability(cap).ifPresent(c -> {
                        if (event.getAmount() > c.getDamageThreshold())
                            c.setAngryAt(src, MobAngerReason.THORNS.get());
                    });
                }
                else {
                    event.getEntity().getCapability(cap).ifPresent(c -> c.setAngryAt(src,
                            event.getAmount() > c.getDamageThreshold() ? MobAngerReason.ATTACKED.get() : MobAngerReason.HIT.get()));
                    src.getCapability(cap).ifPresent(c -> c.setAngryAt(event.getEntity(),
                            event.getAmount() > c.getDamageThreshold() ? MobAngerReason.ATTACKING.get() : MobAngerReason.HITTING.get()));
                }
            }
            EntityComponentAPI.getComponentManager(event.getEntity()).getAllDownstreamComponents().stream()
                .filter(c -> c instanceof MobAngerHandlerComponent).map(c -> (MobAngerHandlerComponent)c)
                .forEach(c -> {
                    if (event.getSource().is(DamageTypes.THORNS)) {
                        if (event.getAmount() > c.getDamageThreshold())
                            c.setAngryAt(src, MobAngerReason.THORNS.get());
                    }
                    else {
                        c.setAngryAt(src, event.getAmount() > c.getDamageThreshold() ? MobAngerReason.ATTACKED.get() : MobAngerReason.HIT.get());
                        c.setAngryAt(event.getEntity(), event.getAmount() > c.getDamageThreshold() ? MobAngerReason.ATTACKING.get() : MobAngerReason.HITTING.get());
                    }
                });
        }
    }
}
