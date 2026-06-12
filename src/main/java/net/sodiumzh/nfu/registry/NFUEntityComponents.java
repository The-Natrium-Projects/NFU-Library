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

public class NFUEntityComponents {

    public static final NFURegistryEntryCollection<EntityComponentType<?, ?>> COLLECTION =
        NFURegistryEntryCollection.create(NFURegistries.ENTITY_COMPONENT_TYPES, NFULibrary.MOD_ID);

    public static final NFURegistry.Accessor<EntityComponentType<Mob, MobAngerHandlerComponent>>
        DEFAULT_ANGER_HANDLER = COLLECTION.register("default_anger_handler", () ->
        new EntityComponentType<>(Mob.class, MobAngerHandlerComponent.class, mob -> new MobAngerHandlerComponent(mob, MobAngerRules.ATTACKER.get())));
    public static final NFURegistry.Accessor<EntityComponentType<LivingEntity, EntityAttributeMonitorComponent>>
        ATTRIBUTE_MONITOR = COLLECTION.register("attribute_monitor", () ->
        new EntityComponentType<>(LivingEntity.class, EntityAttributeMonitorComponent.class, EntityAttributeMonitorComponent.Default::new));
    public static final NFURegistry.Accessor<EntityComponentType<Entity, EntityItemStackMonitorComponent>>
        ITEM_STACK_MONITOR = COLLECTION.register("item_stack_monitor", () ->
        new EntityComponentType<>(Entity.class, EntityItemStackMonitorComponent.class, EntityItemStackMonitorComponent.Default::new));
    public static final NFURegistry.Accessor<EntityComponentType<LivingEntity, HealingHandlerComponent>>
        HEALING_HANDLER = COLLECTION.register("healing_handler", () ->
        new EntityComponentType<>(LivingEntity.class, HealingHandlerComponent.class, HealingHandlerComponent::new));

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = NFULibrary.MOD_ID)
    public static class Attachment {

        @SubscribeEvent
        public static void onInitComponents(EntityComponentSetupEvent event) {
            if (event.getEntity() instanceof Mob mob)
                event.addComponent("/default_anger_handler", NFUEntityComponents.DEFAULT_ANGER_HANDLER.get());
        }

    }
}
