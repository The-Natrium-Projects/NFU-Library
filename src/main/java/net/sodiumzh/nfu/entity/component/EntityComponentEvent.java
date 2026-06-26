package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

public abstract class EntityComponentEvent<T extends Entity, C extends IEntityComponent<?>> extends NFUEntityEvent<T> {

    private final C component;

    public EntityComponentEvent(T entity, C component) {
        super(entity);
        this.component = component;
    }

    public EntityComponentEvent(C component) {
        this((T)(component.getEntity()), component);
    }

    public C getComponent() {
        return component;
    }
}
