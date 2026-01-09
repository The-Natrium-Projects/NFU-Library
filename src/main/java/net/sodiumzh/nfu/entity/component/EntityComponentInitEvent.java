package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

public class EntityComponentInitEvent extends NFUEntityEvent<Entity> {

    private final CEntityComponentManager manager;

    public EntityComponentInitEvent(Entity entity, CEntityComponentManager manager) {
        super(entity);
        this.manager = manager;
    }

    public CEntityComponentManager getComponentManager() {
        return manager;
    }
}
