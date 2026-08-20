package net.sodiumzh.nfu.entity.component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.common.MinecraftForge;
import net.neoforged.common.capabilities.CapabilityDispatcher;
import net.neoforged.common.capabilities.CapabilityProvider;
import net.neoforged.common.capabilities.ICapabilityProvider;
import net.neoforged.event.AttachCapabilitiesEvent;
import net.neoforged.event.TickEvent;
import net.neoforged.event.entity.EntityJoinLevelEvent;
import net.neoforged.eventbus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.capability.NFUEntitySerializableCapProvider;
import net.sodiumzh.nfu.entity.component.preset.EntityDataComponent;
import net.sodiumzh.nfu.entity.component.preset.EntitySyncherComponent;
import net.sodiumzh.nfu.entity.component.preset.IEntityComponentAccess;
import net.sodiumzh.nfu.exception.ReflectionFailedException;
import net.sodiumzh.nfu.mixin.event.entity.EntityFinishConstructionEvent;
import net.sodiumzh.nfu.object.HierarchyPath;
import net.sodiumzh.nfu.reflection.CachedFieldSearchers;
import net.sodiumzh.nfu.registry.NFUConfigs;
import net.sodiumzh.nfu.util.NFUDebugStatics;
import net.sodiumzh.nfu.util.NFUEntityStatics;
import net.sodiumzh.nfu.util.NFUReflectionStatics;

import java.lang.reflect.Field;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = NFULibrary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityComponentEventListeners {

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        event.addCapability(new ResourceLocation(NFULibrary.MOD_ID, "entity_component_manager"),
            new NFUEntitySerializableCapProvider<>(event.getObject(),
                EntityComponentStatics.CAP_MANAGER, () -> new CEntityComponentManagerImpl(event.getObject())));
    }

    @SubscribeEvent
    public static void createDefaultComponents(EntityComponentSetupEvent event) {
        event.addComponent(EntityComponentTypes.PATH_DEFAULT_DATA, EntityComponentTypes.DATA.get());
        event.addComponent(EntityComponentTypes.PATH_DEFAULT_TIMER, EntityComponentTypes.TIMER.get());
        event.addComponent(EntityComponentTypes.PATH_DEFAULT_SYNCHER, EntityComponentTypes.SYNCHER.get());
        if (event.getEntity() instanceof LivingEntity)
            event.addComponent(EntityComponentTypes.PATH_ATTRIBUTE_MONITOR, EntityComponentTypes.ATTRIBUTE_MONITOR.get());
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        event.getEntity().getCapability(EntityComponentStatics.CAP_MANAGER).ifPresent(IEntityComponent::joinLevel);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide() && event.level instanceof ServerLevel sl) {
            if (event.phase.equals(TickEvent.Phase.START))
                EntitySyncherComponent.syncAll(sl, false);
        }
    }

}
