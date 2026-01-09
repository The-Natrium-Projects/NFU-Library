package net.sodiumzh.nfu.entity.component;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.annotation.DontCallManually;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Default base implementation of {@link IEntityComponent} for use in entity-component trees.
 * <p>
 * This abstract base class provides the foundational infrastructure for component trees,
 * including parent-child bookkeeping, required type enforcement, cycle checking, and path-based access.
 * All stateful features such as managing subcomponents, parent assignment, and required subcomponent tracking
 * are handled by this class. Subclass implementations are responsible only for per-component logic such as
 * ticking, serialization, and deserialization.
 * <p>
 * <b>Usage:</b> Component authors are strongly encouraged to subclass {@code EntityComponentBase} rather than implement {@link IEntityComponent} from scratch,
 * unless advanced requirements demand custom storage or field policies. All logic tied to physical fields (e.g. managing the parent reference)
 * is provided here to ensure correct operation.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Maintains an immutable parent reference and provides consistent management of hierarchical relationships, including cycle prevention.</li>
 *   <li>Manages a mapping of (string) subcomponent names to child components, with slash-normalized path handling for easy navigation and manipulation.</li>
 *   <li>Ensures that subcomponent names do not contain slashes to eliminate path ambiguity.</li>
 *   <li>Supports "required" subcomponents by type and path, allowing automatic enforcement and presence checks during runtime.</li>
 *   <li>Leverages parent update hooks (via {@code updateParent}) for correct state mutation without requiring direct field access.</li>
 *   <li>All convenience methods for path browsing, required-subtree assertion, and batch navigation are available via the interface default methods.</li>
 * </ul>
 */
public abstract class EntityComponentBase implements IEntityComponent {
    @Nullable protected IEntityComponent parent;
    protected final Map<String, IEntityComponent> subComponents = new HashMap<>();
    protected final Entity entity;
    protected final Map<String, EntityComponentType<?>> required = new HashMap<>();

    public EntityComponentBase(Entity entity) {
        if (entity == null)
            throw new IllegalArgumentException("Entity cannot be null for EntityComponentBase.");
        this.entity = entity;
    }

    @Override
    public Optional<IEntityComponent> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public void addSubComponent(@Nonnull String name, @Nonnull IEntityComponent component) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Subcomponent name must not be null or empty.");
        if (name.contains("\\") || name.contains("/"))
            throw new IllegalArgumentException("'\\' and '/' are preserved for paths, and not allowed in names.");
        if (component == null)
            throw new IllegalArgumentException("Component cannot be null.");
        if (component == this)
            throw new IllegalArgumentException("Cannot add self as subcomponent.");
        if (component.getParent().isPresent())
            throw new IllegalArgumentException("Component already has a parent. Detach first.");
        if (subComponents.containsKey(name))
            throw new IllegalArgumentException("Duplicate subcomponent name. Use replaceSubComponent instead.");
        if (createsCycle(component))
            throw new IllegalStateException("Cycle detected: adding would produce a cyclic tree.");
        subComponents.put(name, component);
        // Important: update parent for the child and call updateParent
        component.updateParent(null, this);
        ((EntityComponentBase) component).parent = this;
    }

    @Nullable
    @Override
    public IEntityComponent removeSubComponent(String name) {
        IEntityComponent removed = subComponents.remove(name);
        if (removed != null) {
            removed.updateParent(this, null);
        }
        return removed;
    }

    @Override
    public Map<String, IEntityComponent> getSubComponents() {
        return Collections.unmodifiableMap(subComponents);
    }

    /**
     * Returns true if adding the given component would cause a cycle.
     */
    protected boolean createsCycle(IEntityComponent candidate) {
        IEntityComponent curr = this;
        while (curr != null) {
            if (curr == candidate) {
                return true;
            }
            curr = ((EntityComponentBase) curr).parent;
        }
        return false;
    }

    @Override
    @DontCallManually
    public void updateParent(@Nullable IEntityComponent oldParent, @Nullable IEntityComponent newParent) {
        this.parent = newParent;
    }

    public void setRequired(String path, EntityComponentType<?> type) {
        // Uniformize the format of path
        String pathKey = Arrays.stream(path.split("[/\\\\]")).filter(s -> !s.isEmpty())
            .map(s -> "\\" + s).reduce("", (s1, s2) -> s1 + s2);
        this.required.put(pathKey, type);
        @Nullable IEntityComponent old = this.getSubComponentByPath(pathKey).orElse(null);
        if (old != null && !old.getType().equals(type))
            throw new IllegalStateException("Failed to set required sub-component at "
            + pathKey + "\" because it's occupied by a component of another type: \""
            + old.getType().getKey().toString() + "\".");
        if (this.getSubComponentByPath(pathKey).isEmpty())
            this.addSubComponentByPath(pathKey, type.factory().create(this.getEntity()));
    }

    public Optional<EntityComponentType<?>> getTypeIfRequired(String path) {
        // Uniformize the format of path
        String pathKey = Arrays.stream(path.split("[/\\\\]")).filter(s -> !s.isEmpty())
            .map(s -> "\\" + s).reduce("", (s1, s2) -> s1 + s2);
        return Optional.ofNullable(this.required.get(pathKey));
    }

    public Map<String, EntityComponentType<?>> getAllRequired() {
        return Map.copyOf(this.required);
    }

}