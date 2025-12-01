package net.sodiumzh.nfu.effect;


import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.math.LinearColor;

public class EmptyEffect extends MobEffect {

    public EmptyEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    public EmptyEffect() {
        this(MobEffectCategory.NEUTRAL, LinearColor.fromRGB(127, 127, 127).toCode());
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = NFULibrary.MOD_ID)
    public static class EventListeners {
        @SubscribeEvent
        public static void handle(MobEffectEvent.Applicable event) {
            if (event.getEffectInstance().getEffect() instanceof EmptyEffect)
                event.setResult(Event.Result.DENY);
        }
    }
}
