package net.sodiumzh.nfu.registry;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.entity.anger.MobAngerHandlerComponent;
import net.sodiumzh.nfu.entity.anger.MobAngerRules;
import net.sodiumzh.nfu.entity.component.EntityComponentSetupEvent;
import net.sodiumzh.nfu.entity.component.EntityComponentType;

public class NFUEntityComponents {

    public static final NFURegistryEntryCollection<EntityComponentType<?, ?>> COLLECTION =
        NFURegistryEntryCollection.create(NFURegistries.ENTITY_COMPONENT_TYPES, NFULibrary.MOD_ID);

    public static final NFURegistry.Accessor<EntityComponentType<Mob, MobAngerHandlerComponent>>
        DEFAULT_ANGER_HANDLER = COLLECTION.register("default_anger_handler", () ->
        new EntityComponentType<>(Mob.class, MobAngerHandlerComponent.class, mob -> new MobAngerHandlerComponent(mob, MobAngerRules.ATTACKER.get())));

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = NFULibrary.MOD_ID)
    public static class Attachment {

        @SubscribeEvent
        public static void onInitComponents(EntityComponentSetupEvent event) {
            if (event.getEntity() instanceof Mob mob)
                event.addComponent("/default_anger_handler", NFUEntityComponents.DEFAULT_ANGER_HANDLER.get());
        }

    }
}
