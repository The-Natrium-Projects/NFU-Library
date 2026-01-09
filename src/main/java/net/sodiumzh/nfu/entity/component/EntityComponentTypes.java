package net.sodiumzh.nfu.entity.component;

import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.registry.NFURegistry;
import net.sodiumzh.nfu.registry.NFURegistryEntryCollection;

import javax.swing.text.html.parser.Entity;

public class EntityComponentTypes {

    public static final NFURegistryEntryCollection<EntityComponentType<?>> COLLECTION =
        NFURegistryEntryCollection.create(NFURegistries.ENTITY_COMPONENT_TYPES, NFULibrary.MOD_ID);

    public static final NFURegistry.Accessor<EntityComponentType<CEntityComponentManager>> ROOT =
            COLLECTION.register("root", () ->
                    new EntityComponentType<>(CEntityComponentManager.class, CEntityComponentManager::factory));
    public static final NFURegistry.Accessor<EntityComponentType<EntityDynamicDataComponent>> DYNAMIC_DATA =
            COLLECTION.register("dynamic_data", () ->
                    new EntityComponentType<>(EntityDynamicDataComponent.class, EntityDynamicDataComponent::new));
    public static final NFURegistry.Accessor<EntityComponentType<DefaultEntityTimerComponent>> DEFAULT_TIMER =
            COLLECTION.register("default_timer", () ->
                    new EntityComponentType<>(DefaultEntityTimerComponent.class, DefaultEntityTimerComponent::new));
}
