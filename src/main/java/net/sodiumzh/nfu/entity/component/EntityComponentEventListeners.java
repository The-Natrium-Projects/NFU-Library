package net.sodiumzh.nfu.entity.component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.capability.NFUEntitySerializableCapProvider;
import net.sodiumzh.nfu.mixin.event.entity.EntityFinishConstructionEvent;
import net.sodiumzh.nfu.util.NFUDebugStatics;

@Mod.EventBusSubscriber(modid = NFULibrary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityComponentEventListeners {

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        event.addCapability(new ResourceLocation(NFULibrary.MOD_ID, "entity_component_manager"),
            new NFUEntitySerializableCapProvider<>(event.getObject(),
                EntityComponentStatics.CAP_MANAGER, () -> new CEntityComponentManagerImpl(event.getObject())));
    }

    @SubscribeEvent
    public static void initComponentMgr(EntityFinishConstructionEvent event) {
        event.getEntity().getCapability(EntityComponentStatics.CAP_MANAGER).resolve().ifPresentOrElse(m -> {
            if (event.getEntity() instanceof IEntityComponentManagerHolder holder)
                holder.initializeComponents(m);
            createDefaultComponents(event.getEntity(), m);
            MinecraftForge.EVENT_BUS.post(new EntityComponentInitEvent(event.getEntity(), m));
        }, () -> { NFUDebugStatics.errorOnce(EntityComponentEventListeners.class,
            String.format("%s Missing entity component manager", event.getEntity().getName().getString())); });
    }

    private static void createDefaultComponents(Entity e, CEntityComponentManager mgr) {
        mgr.setRequired("/dynamic_data", EntityComponentTypes.DYNAMIC_DATA.get());
        mgr.setRequired("/default_timer", EntityComponentTypes.DEFAULT_TIMER.get());
        mgr.setRequired("/default_syncher", EntityComponentTypes.DEFAULT_SYNCHER.get());
    }
}
