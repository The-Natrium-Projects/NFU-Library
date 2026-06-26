package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.entity.component.preset.*;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.registry.NFURegistry;
import net.sodiumzh.nfu.registry.NFURegistryEntryCollection;


public class EntityComponentTypes {

    public static final NFURegistryEntryCollection<EntityComponentType<?, ?>> COLLECTION =
        NFURegistryEntryCollection.create(NFURegistries.ENTITY_COMPONENT_TYPES, NFULibrary.MOD_ID);

    public static final NFURegistry.Accessor<EntityComponentType<Entity, CEntityComponentManager>> ROOT =
        COLLECTION.register("root", () ->
            new EntityComponentType<>(Entity.class, CEntityComponentManager.class, CEntityComponentManager::factory));
    public static final NFURegistry.Accessor<EntityComponentType<Entity, EntityDataComponent<Entity>>> DATA =
        COLLECTION.register("data", () ->
            new EntityComponentType<>(Entity.class, EntityDataComponent.class, EntityDataComponent::new));
    public static final NFURegistry.Accessor<EntityComponentType<Entity, EntityTimerComponent<Entity>>> TIMER =
        COLLECTION.register("timer", () ->
            new EntityComponentType<>(Entity.class, EntityTimerComponent.class, EntityTimerComponent::new));
    public static final NFURegistry.Accessor<EntityComponentType<Entity, EntitySyncherComponent<Entity>>> SYNCHER =
        COLLECTION.register("syncher", () ->
            new EntityComponentType<>(Entity.class, EntitySyncherComponent.class, EntitySyncherComponent::new));
    public static final NFURegistry.Accessor<EntityComponentType<Entity, EntityNodeComponent>> NODE =
        COLLECTION.register("node", () ->
            new EntityComponentType<>(Entity.class, EntityNodeComponent.class, EntityNodeComponent::new));
    public static final NFURegistry.Accessor<EntityComponentType<LivingEntity, EntityAttributeMonitorComponent>>
        ATTRIBUTE_MONITOR = COLLECTION.register("attribute_monitor", () ->
        new EntityComponentType<>(LivingEntity.class, EntityAttributeMonitorComponent.class, EntityAttributeMonitorComponent.Default::new));
    public static final NFURegistry.Accessor<EntityComponentType<Entity, EntityItemStackMonitorComponent>>
        ITEM_STACK_MONITOR = COLLECTION.register("item_stack_monitor", () ->
        new EntityComponentType<>(Entity.class, EntityItemStackMonitorComponent.class, EntityItemStackMonitorComponent.Default::new));
    public static final NFURegistry.Accessor<EntityComponentType<LivingEntity, HealingHandlerComponent>>
        HEALING_HANDLER = COLLECTION.register("healing_handler", () ->
        new EntityComponentType<>(LivingEntity.class, HealingHandlerComponent.class, HealingHandlerComponent::new));
}
