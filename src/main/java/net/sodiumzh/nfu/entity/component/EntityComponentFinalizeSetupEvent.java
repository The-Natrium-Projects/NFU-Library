package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

import java.util.Optional;

public class EntityComponentFinalizeSetupEvent extends NFUEntityEvent<Entity> {

    private final CEntityComponentManager manager;

    public EntityComponentFinalizeSetupEvent(Entity entity, CEntityComponentManager mgr) {
        super(entity);
        this.manager = mgr;
    }

    public CEntityComponentManager getComponentManager() {
        return this.manager;
    }

    public <T extends IEntityComponent<?>> Optional<T> getComponentByPath(String path, EntityComponentType<?, T> type) {
        return this.getComponentManager().getSubComponentByPath(path, type);
    }

    public Optional<IEntityComponent<?>> getComponentByPath(String path) {
        return this.getComponentManager().getSubComponentByPath(path);
    }

}
