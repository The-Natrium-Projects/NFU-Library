package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.registry.NFURegistry;
import net.sodiumzh.nfu.registry.NFURegistryEntryCollection;


public class EntityComponentTypes {

    public static final NFURegistryEntryCollection<EntityComponentType<?, ?>> COLLECTION =
        NFURegistryEntryCollection.create(NFURegistries.ENTITY_COMPONENT_TYPES, NFULibrary.MOD_ID);

    public static final NFURegistry.Accessor<EntityComponentType<Entity, CEntityComponentManager>> ROOT =
        COLLECTION.register("root", () ->
            new EntityComponentType<>(Entity.class, CEntityComponentManager.class, CEntityComponentManager::factory));
    public static final NFURegistry.Accessor<EntityComponentType<Entity, EntityDynamicDataComponent<Entity>>> DYNAMIC_DATA =
        COLLECTION.register("dynamic_data", () ->
            new EntityComponentType<>(Entity.class, EntityDynamicDataComponent.class, EntityDynamicDataComponent::new));
    public static final NFURegistry.Accessor<EntityComponentType<Entity, EntityTimerComponent<Entity>>> DEFAULT_TIMER =
        COLLECTION.register("default_timer", () ->
            new EntityComponentType<>(Entity.class, EntityTimerComponent.class, EntityTimerComponent::new));
    public static final NFURegistry.Accessor<EntityComponentType<Entity, EntitySyncherComponent<Entity>>> DEFAULT_SYNCHER =
        COLLECTION.register("default_syncher", () ->
            new EntityComponentType<>(Entity.class, EntitySyncherComponent.class, EntitySyncherComponent::new));
}
