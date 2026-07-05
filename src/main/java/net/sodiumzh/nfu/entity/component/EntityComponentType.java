package net.sodiumzh.nfu.entity.component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.network.AvailableSide;
import net.sodiumzh.nfu.registry.NFURegistries;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * @param componentClass Component class for this type.
 * @param factory Default method to generate the component from an entity.
 * @param <E> Entity type that the factory method receives.
 * @param <T> Component type that the factory method outputs.
 * @param factory method to generate component from entity. No need to consider type.
 */
public record EntityComponentType<E extends Entity, T extends IEntityComponent<E>>(
    @Nonnull Class<? extends E> entityClass,
    @Nonnull Class<? extends T> componentClass,
    @Nonnull AvailableSide availableSide,
    @Nonnull Function<E, T> factory) {

    public EntityComponentType(Class<? extends E> entityClass, Class<? extends T> componentClass, Function<E, T> factory) {
        this(entityClass, componentClass, AvailableSide.BOTH, factory);
    }

    public ResourceLocation getKey() {
        ResourceLocation res = NFURegistries.ENTITY_COMPONENT_TYPES.getKey(this);
        if (res != null) return res;
        else
            throw new IllegalStateException("Dangling EntityComponentType. Must be registered in NFURegistries.ENTITY_COMPONENT_TYPES.");
    }

    /**
     * Create component from entity. Always use this to create components, and don't call raw constructors as it doesn't
     * initialize type, unless you do this manually!!
     */
    public T create(E entity, boolean serialized) {
        this.availableSide.assertCorrectSide(entity);
        T res = this.factory().apply(entity);
        res.setType(this);
        res.setSerialize(serialized);
        return res;
    }

    /**
     * Create component from entity. Always use this to create components, and don't call raw constructors as it doesn't
     * initialize type, unless you do this manually!!
     */
    public T create(E entity) {
        return create(entity, true);
    }

    /**
     * Create the default component from a raw Entity reference. This will detect if type matches and throw if not.
     *
     * @param e Entity parameter.
     * @return New component.
     */
    public T createUnsafe(Entity e, boolean serialized) {
        if (this.entityClass().isAssignableFrom(e.getClass())) {
            this.availableSide.assertCorrectSide(e);
            return this.create((E) e, serialized);
        }
        else
            throw new IllegalArgumentException("Entity type mismatch: creating component type " + this.componentClass().getName()
                + " for entity class " + this.entityClass().getName() + ", but input entity is " + e.getClass().getName());
    }

    /**
     * Create the default component from a raw Entity reference. This will detect if type matches and throw if not.
     * @param e Entity parameter.
     * @return New component.
     */
    public T createUnsafe(Entity e) {
        return createUnsafe(e, true);
    }

}

