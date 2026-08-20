package net.sodiumzh.nfu.eventhandler;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.event.entity.EntityAttributeCreationEvent;
import net.neoforged.eventbus.api.EventPriority;
import net.neoforged.eventbus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.entity.DeferredEntityAttributeRegisterEvent;
import net.sodiumzh.nfu.network.NFUDataSerializer;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.registry.NFURegistry;
import net.sodiumzh.nfu.registry.NFURegistryGenerateValuesEvent;

import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = NFULibrary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NFUSetupEventHandlers {

    /**
     * Generate registry values if needed.
     * @see NFURegistry
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void generateRegistries(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            NFURegistry.COMMON_SETUP_DONE.trySet(true);
            List<NFURegistry<?>> shouldGenerate = NFURegistry.allRegistries().values().stream()
                    .filter(reg -> reg.getLoadTiming().equals(NFURegistry.LoadTiming.COMMON_SETUP))
                    .toList();
            shouldGenerate = NFURegistry.sortByLoadingOrder(shouldGenerate);
            shouldGenerate.forEach(reg -> ModLoader.get().postEvent(new NFURegistryGenerateValuesEvent.CommonBefore(reg)));
            shouldGenerate.forEach(NFURegistry::load);
            shouldGenerate.forEach(reg -> ModLoader.get().postEvent(new NFURegistryGenerateValuesEvent.CommonAfter(reg)));
        });
    }

    @SubscribeEvent
    public static void registerDeferredAttributeSuppliers(EntityAttributeCreationEvent event)
    {
        ModLoader.get().postEvent(new DeferredEntityAttributeRegisterEvent());
    }

    @SubscribeEvent
    public static void onGenerateRegistries(NFURegistryGenerateValuesEvent.CommonAfter event) {
        /**
         * Auto register existing
         */
        if (event.registry.equals(NFURegistries.DATA_SERIALIZERS)) {
            NFURegistry<NFUDataSerializer<?>> reg = NFURegistries.DATA_SERIALIZERS;
            // First check if maunally-registered list and optional serializers have valid types
            reg.keySet().stream().map(key -> new AbstractMap.SimpleEntry<>(key, reg.getValue(key)))
                .filter(entry -> entry.getValue() != null
                    && entry.getKey().getPath().endsWith("_list")
                    && !entry.getValue().getObjectClass().equals(List.class))
                    .findAny().ifPresent(entry -> {throw new IllegalArgumentException("names with \"_list\" path suffix are reserved for list serializers. Error entry: " + entry.getKey().toString());});
            reg.keySet().stream().map(key -> new AbstractMap.SimpleEntry<>(key, reg.getValue(key)))
                .filter(entry -> entry.getValue() != null
                    && entry.getKey().getPath().startsWith("optional_")
                    && !entry.getValue().getObjectClass().equals(Optional.class))
                .findAny().ifPresent(entry -> {throw new IllegalArgumentException("names with \"optional_\" path prefix are reserved for optional serializers. Error entry: " + entry.getKey().toString());});
            // Generate list and collection versions of existing data serializers
            reg.keySet().stream().map(key -> new AbstractMap.SimpleEntry<>(key, reg.getValue(key)))
                .filter(entry -> entry.getValue() != null
                    && !entry.getValue().getObjectClass().equals(List.class)
                    && !entry.getValue().getObjectClass().equals(Optional.class)
                    && !entry.getKey().getPath().endsWith("_list")
                    && !entry.getKey().getPath().startsWith("optional_"))
                .toList().forEach(entry -> {
                    reg.registerIfAbsent(new ResourceLocation(entry.getKey().getNamespace(), entry.getKey().getPath() + "_list"),
                        () -> NFUDataSerializer.createList(entry.getValue()));
                    reg.registerIfAbsent(new ResourceLocation(entry.getKey().getNamespace(), "optional_" + entry.getKey().getPath()),
                        () -> NFUDataSerializer.createOptional(entry.getValue()));
                });
        }
    }

}
