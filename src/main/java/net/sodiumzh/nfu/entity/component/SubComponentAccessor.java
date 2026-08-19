package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.object.HierarchyPath;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A utility including a subcomponent's path and type. For simplifying subcomponent-by-path search.
 */
public class SubComponentAccessor<E extends Entity, T extends IEntityComponent<E>> {

    private final HierarchyPath path;
    // Type is lazy-loaded. Prevent triggering loading here
    private final Supplier<EntityComponentType<E, T>> type;

    public SubComponentAccessor(HierarchyPath path, Supplier<EntityComponentType<E, T>> type) {
        this.path = path;
        this.type = type;
    }

    public HierarchyPath getPath() {
        return path;
    }

    public EntityComponentType<E, T> getType() {
        return type.get();
    }

    public Optional<T> getSubComponent(IEntityComponent<?> e) {
        return e.getSubComponentByPath(this);
    }

}
