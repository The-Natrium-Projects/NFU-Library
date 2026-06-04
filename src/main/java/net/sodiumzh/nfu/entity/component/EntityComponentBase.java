package net.sodiumzh.nfu.entity.component;

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
public abstract class EntityComponentBase<E extends Entity> implements IEntityComponent<E> {

    @Nullable protected IEntityComponent<?> parent;
    protected final Map<String, IEntityComponent<? extends Entity>> subComponents = new HashMap<>();
    protected final E entity;
    protected EntityComponentType<E, ? extends IEntityComponent<E>> type;
    protected boolean serialize = true;

    public EntityComponentBase(E entity) {
        if (entity == null)
            throw new IllegalArgumentException("Entity cannot be null for EntityComponentBase.");
        this.entity = entity;
    }

    @Override
    public Optional<IEntityComponent<?>> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public E getEntity() {
        return entity;
    }

    @Override
    public void addSubComponent(@Nonnull String name, @Nonnull IEntityComponent<? extends Entity> component) {
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
    }

    @Nullable
    @Override
    public IEntityComponent<? extends Entity> removeSubComponent(String name) {
        IEntityComponent<? extends Entity> removed = subComponents.remove(name);
        if (removed != null) {
            removed.updateParent(this, null);
        }
        return removed;
    }

    @Override
    public Map<String, IEntityComponent<? extends Entity>> getSubComponents() {
        return Collections.unmodifiableMap(subComponents);
    }

    /**
     * Returns true if adding the given component would cause a cycle.
     */
    protected boolean createsCycle(IEntityComponent<?> candidate) {
        IEntityComponent<?> curr = this;
        while (curr != null) {
            if (curr == candidate) {
                return true;
            }
            curr = curr.getParent().orElse(null);
        }
        return false;
    }

    @Override
    @DontCallManually
    public void updateParent(@Nullable IEntityComponent<?> oldParent, @Nullable IEntityComponent<?> newParent) {
        this.parent = newParent;
    }

    @Override
    public EntityComponentType<E, ? extends IEntityComponent<E>> getType() {
        if (type == null)
            throw new IllegalStateException("NFU Entity Component: Missing type. Maybe not initialized? Always create component from type instead of directly using new.");
        return type;
    }

    @Override
    public void setType(EntityComponentType<E, ? extends IEntityComponent<E>> type) {
        this.type = type;
    }

    public boolean shouldSerialize() {
        return this.serialize;
    }

    public void setSerialize(boolean shouldSerialize) {
        this.serialize = shouldSerialize;
    }

    // Hierarchy safety check modules //
    // Methods below will be checked each few seconds in the component manager to ensure a valid structure is present.

    /**
     * Get the map of required subcomponent paths and types. It will be checked after component tree initialization.
     */
    public Map<String, EntityComponentType<?, ?>> getRequiredSubcomponents() {
        return Map.of();
    }

    /**
     * Get all legal paths of this component. Keep empty to require no path.
     */
    public List<String> getRequiredPaths() {
        return List.of();
    }

    public void checkHierarchy() {
        String subcomponentErrMsg = getRequiredSubcomponents().entrySet().stream()
            .filter(entry -> this.getSubComponentByPath(entry.getKey(), entry.getValue()).isEmpty())
            .map(entry -> String.format(Locale.ENGLISH, "\"%s\"(%s); ", entry.getKey(), entry.getValue().getKey().toString()))
            .reduce("", String::concat);
        if (!subcomponentErrMsg.isEmpty()) {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "Component \"%s\"(%s) missing subcomponent(s): %s", this.getPathFromRoot(), this.getType().getKey(), subcomponentErrMsg));
        }

        List<String> requiredPathsList = getRequiredPaths().stream().map(IEntityComponent::formatPath).distinct().toList();
        if (!requiredPathsList.contains(this.getPathFromRoot())) {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "Illegal path \"%s\" for component type %s. Valid paths: %s",
                this.getPathFromRoot(), this.getType().getKey(), requiredPathsList.stream().reduce("", (s1, s2) -> s1 + ", " + s2)));
        }
    }

}