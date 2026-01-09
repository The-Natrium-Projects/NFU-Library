package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;

/**
 * Posted at the end of Entity constructor, after gathering capabilities.
 */
public class EntityFinishConstructionEvent extends NFUEntityEvent<Entity> {
    public EntityFinishConstructionEvent(Entity entity) {
        super(entity);
    }
}
