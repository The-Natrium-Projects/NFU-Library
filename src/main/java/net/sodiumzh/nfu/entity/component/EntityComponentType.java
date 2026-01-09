package net.sodiumzh.nfu.entity.component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.registry.NFURegistries;

import javax.annotation.Nonnull;

public record EntityComponentType<T extends IEntityComponent>(@Nonnull Class<T> componentClass,
                                                              @Nonnull IEntityComponentFactory<? extends T> factory) {

    public ResourceLocation getKey() {
        ResourceLocation res = NFURegistries.ENTITY_COMPONENT_TYPES.getKey(this);
        if (res != null) return res;
        else throw new IllegalStateException("Dangling EntityComponentType. Must be registered in NFURegistries.ENTITY_COMPONENT_DESERIALIZERS.");
    }

    @FunctionalInterface
    public static interface IEntityComponentFactory<T extends IEntityComponent> {
        public T create(Entity entity);
    }

}
