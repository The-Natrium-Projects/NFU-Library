package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;

public class DefaultEntityTimerComponent extends EntityTimerComponent {

    public DefaultEntityTimerComponent(Entity entity) {
        super(entity);
    }

    @Override
    public EntityComponentType<?> getType() {
        return EntityComponentTypes.DEFAULT_TIMER.get();
    }

}
