package net.sodiumzh.nfu.registry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.entity.anger.MobAngerHandlerComponent;
import net.sodiumzh.nfu.entity.anger.MobAngerRules;
import net.sodiumzh.nfu.entity.component.EntityComponentSetupEvent;
import net.sodiumzh.nfu.entity.component.EntityComponentType;
import net.sodiumzh.nfu.entity.component.preset.EntityAttributeMonitorComponent;
import net.sodiumzh.nfu.entity.component.preset.EntityItemStackMonitorComponent;
import net.sodiumzh.nfu.entity.component.preset.HealingHandlerComponent;
import net.sodiumzh.nfu.network.AvailableSide;
import net.sodiumzh.nfu.object.HierarchyPath;

public class NFUEntityComponents {

    public static final NFURegistryEntryCollection<EntityComponentType<?, ?>> COLLECTION =
        NFURegistryEntryCollection.create(NFURegistries.ENTITY_COMPONENT_TYPES, NFULibrary.MOD_ID);

    public static final NFURegistry.Accessor<EntityComponentType<Mob, MobAngerHandlerComponent>>
        DEFAULT_ANGER_HANDLER = COLLECTION.register("default_anger_handler", () ->
        new EntityComponentType<>(Mob.class, MobAngerHandlerComponent.class, AvailableSide.SERVER,
            mob -> new MobAngerHandlerComponent(mob, MobAngerRules.ATTACKER.get())));

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = NFULibrary.MOD_ID)
    public static class Attachment {

        @SubscribeEvent
        public static void onInitComponents(EntityComponentSetupEvent event) {
            if (event.getEntity() instanceof Mob mob)
                event.addComponent(HierarchyPath.byNameArray("default_anger_handler"), NFUEntityComponents.DEFAULT_ANGER_HANDLER.get());
        }
    }
}
