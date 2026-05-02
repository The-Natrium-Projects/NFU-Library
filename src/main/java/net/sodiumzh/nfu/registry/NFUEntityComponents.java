package net.sodiumzh.nfu.registry;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.entity.anger.MobAngerHandlerComponent;
import net.sodiumzh.nfu.entity.component.EntityComponentInitEvent;
import net.sodiumzh.nfu.entity.component.EntityComponentType;

public class NFUEntityComponents {

    public static final NFURegistryEntryCollection<EntityComponentType<?, ?>> COLLECTION =
        NFURegistryEntryCollection.create(NFURegistries.ENTITY_COMPONENT_TYPES, NFULibrary.MOD_ID);

    public static final NFURegistry.Accessor<EntityComponentType<Mob, MobAngerHandlerComponent.Default>>
        DEFAULT_ANGER_HANDLER = COLLECTION.register("default_anger_handler", () ->
        new EntityComponentType<>(Mob.class, MobAngerHandlerComponent.Default.class, MobAngerHandlerComponent.Default::new));

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = NFULibrary.MOD_ID)
    public static class Attachment {

        @SubscribeEvent
        public static void onInitComponents(EntityComponentInitEvent event) {
            event.getComponentManager().setRequiredIfClassMatches("/default_anger_handler", NFUEntityComponents.DEFAULT_ANGER_HANDLER.get());
        }

    }
}
