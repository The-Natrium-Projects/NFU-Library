package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.util.NFUDebugStatics;

public class EntityComponentAPI {

    public static CEntityComponentManager getComponentManager(Entity e) {
        return e.getCapability(EntityComponentStatics.CAP_MANAGER).orElseGet(() -> {
            NFUDebugStatics.errorOnce(EntityComponentAPI.class, String.format("Entity \"%s\" missing component manager. " +
                    "If the entity is pending removal, this message can be ignored.", e.getName().getString()));
            return new CEntityComponentManagerImpl(e);
        });
    }

    public static EntityDynamicDataComponent getDynamicDataComponent(Entity e) {
        return getComponentManager(e).getSubComponent("dynamic_data", EntityComponentTypes.DYNAMIC_DATA.get()).orElseGet(() -> {
            NFUDebugStatics.errorOnce(EntityComponentAPI.class, String.format("Entity \"%s\" missing dynamic data component. " +
                    "If the entity is pending removal, this message can be ignored.", e.getName().getString()));
            return new EntityDynamicDataComponent(e);
        });
    }

    public static DefaultEntityTimerComponent getDefaultTimer(Entity e) {
        return getComponentManager(e).getSubComponent("default_timer", EntityComponentTypes.DEFAULT_TIMER.get()).orElseGet(() -> {
            NFUDebugStatics.errorOnce(EntityComponentAPI.class, String.format("Entity \"%s\" missing default timer component. " +
                    "If the entity is pending removal, this message can be ignored.", e.getName().getString()));
            return new DefaultEntityTimerComponent(e);
        });
    }
}
