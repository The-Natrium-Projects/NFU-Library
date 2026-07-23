package net.sodiumzh.nfu.entity.component;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.annotation.DontCallManually;
import net.sodiumzh.nfu.object.HierarchyPath;
import net.sodiumzh.nfu.util.NFUContainerStatics;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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
    protected final BiMap<String, IEntityComponent<? extends Entity>> subComponents = HashBiMap.create();
    protected final E entity;
    protected EntityComponentType<E, ? extends IEntityComponent<E>> type;
    protected boolean serialize = true;
    protected boolean rebuildOnDeserialization = false;

    public EntityComponentBase(E entity) {
        if (entity == null)
            throw new IllegalArgumentException("Entity cannot be null for EntityComponentBase.");
        this.entity = entity;
    }

    @Override
    public Optional<IEntityComponent<?>> getParent() {
        return Optional.ofNullable(parent);
    }


    public HierarchyPath getPathFromRoot() {
        StringBuilder literal = new StringBuilder();
        IEntityComponent<?> current = this;
        while (true) {
            IEntityComponent<?> parent = current.getParent().orElse(null);
            if (parent == null) return HierarchyPath.byLiteral(literal.toString());
            literal.insert(0, "/" + parent.getSubComponentName(current).orElseThrow());
            current = parent;
        }
    }

    @Override
    public Optional<HierarchyPath> getPathFrom(IEntityComponent<?> upstreamComponent) {
        StringBuilder literal = new StringBuilder();
        IEntityComponent<?> current = this;
        while (current != upstreamComponent) {
            IEntityComponent<?> parent = current.getParent().orElse(null);
            if (parent == null) return Optional.empty();
            literal.insert(0, "/" + parent.getSubComponentName(current).orElseThrow());
            current = parent;
        }
        return Optional.of(HierarchyPath.byLiteral(literal.toString()));
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
        subComponents.put(name, component);
        // Important: update parent for the child and call updateParent
        component.updateParent(null, this);
        component.pathDepth();  // This checks cyclic hierarchy dependency
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
    public void addSubComponentByPath(HierarchyPath path, IEntityComponent<? extends Entity> component, boolean fillNodesIfParentAbsent) {
        HierarchyPath parentOfAdded = path.getParent();
        if (parentOfAdded == null) throw new IllegalArgumentException("Path is empty");

        AtomicReference<IEntityComponent<?>> current = new AtomicReference<>(this);
        for (int i = 0; i < parentOfAdded.length(); ++i) {
            int i1 = i;
            current.get().getSubComponent(parentOfAdded.getAt(i)).ifPresentOrElse(current::set, () -> {
                if (fillNodesIfParentAbsent) {
                    EntityNodeComponent node = EntityComponentTypes.NODE.get().create(this.getEntity());
                    current.get().addSubComponent(parentOfAdded.getAt(i1), node);
                    current.set(node);
                } else {
                    throw new IllegalStateException("Attempting to add component at path " + path.toLiteral()
                        + ", but missing parent node "
                        + HierarchyPath.formatLiteral(Arrays.stream(path.toStringArray()).limit(i1 + 1).toArray(String[]::new)) + ".");
                }
            });
        }
        // Now current is its direct parent
        current.get().addSubComponent(path.getAt(path.length() - 1), component);
    }

    @Override
    public Map<String, IEntityComponent<? extends Entity>> getSubComponents() {
        return Collections.unmodifiableMap(subComponents);
    }

    @Override
    public Optional<IEntityComponent<? extends Entity>> getSubComponent(String name) {
        return Optional.ofNullable(this.subComponents.get(name));
    }

    @Override
    public Optional<String> getSubComponentName(@Nonnull IEntityComponent<? extends Entity> subComponent) {
        return Optional.ofNullable(this.subComponents.inverse().get(subComponent));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void collectDownstreamComponentsTo(@Nonnull Set<IEntityComponent> outSet) {
        for (IEntityComponent child : getSubComponents().values()) {
            outSet.add(child);
            child.collectDownstreamComponentsTo(outSet);
        }
    }

    @Override
    public Map<HierarchyPath, IEntityComponent<?>> getAllPathsAndDownstreamComponents() {
        Map<HierarchyPath, IEntityComponent<?>> res = new HashMap<>();
        this.getSubComponents().forEach((k, v) -> {
            res.put(HierarchyPath.byNameArray(k), v);
            String[] thisKey = new String[]{k};
            v.getAllPathsAndDownstreamComponents().forEach((k1, v1) -> {
                res.put(HierarchyPath.byNameArray(NFUContainerStatics.concatArray(thisKey, k1.toStringArray(), String[]::new)), v1);
            });
        });
        return res;
    }

    @Override
    public Optional<IEntityComponent<? extends Entity>> getSubComponentByPath(HierarchyPath path) {
        Optional<IEntityComponent<? extends Entity>> res = Optional.of(this);
        for (String name: path.toStringArray()) {
            res = res.flatMap(c -> c.getSubComponent(name));
        }
        return res;
    }

    /**
     * Returns true if adding the given component would cause a cycle. Only for internal cycle dependency check.
     */
    @ApiStatus.Internal
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

    @Override
    public IEntityComponent<?> getRoot() {
        IEntityComponent<?> current = this;
        while (true) {
            IEntityComponent<?> parent = current.getParent().orElse(null);
            if (parent == null) return current;
            current = parent;
        }
    }

    @Override
    public boolean shouldSerialize() {
        return this.serialize;
    }

    public void setSerialize(boolean shouldSerialize) {
        this.serialize = shouldSerialize;
    }

    @Override
    public boolean shouldRebuildOnDeserialization() {
        return rebuildOnDeserialization;
    }

    @Override
    public void joinLevel() {}

    public void setRebuildOnDeserialization(boolean value) {
        this.rebuildOnDeserialization = value;
    }

    // Hierarchy safety check module //
    // Methods below will be checked each few seconds in the component manager to ensure a valid structure is present.

    /**
     * Get the map of required subcomponent paths and types. It will be checked after component tree initialization.
     */
    public Map<HierarchyPath, EntityComponentType<?, ?>> getRequiredSubcomponents() {
        return Map.of();
    }

    /**
     * If this component requires a specific path from root, return the list of all allowed paths.
     * Return null or empty list for no path requirement.
     */
    @Nullable
    public List<HierarchyPath> getAllowedPaths() {
        return null;
    }

    public void checkHierarchy() {
        // Check if required subcomponents are present
        String subcomponentErrMsg = getRequiredSubcomponents().entrySet().stream()
            .filter(entry -> this.getSubComponentByPath(entry.getKey(), entry.getValue()).isEmpty())
            .map(entry -> String.format(Locale.ENGLISH, "\"%s\"(%s); ", entry.getKey(), entry.getValue().getKey().toString()))
            .reduce("", String::concat);
        if (!subcomponentErrMsg.isEmpty()) {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "Component \"%s\"(%s) missing subcomponent(s): %s", this.getPathFromRoot(), this.getType().getKey(), subcomponentErrMsg));
        }
        // Check if own path from root is legal
        List<HierarchyPath> allowedPaths = getAllowedPaths();
        if (allowedPaths != null && !allowedPaths.isEmpty() && !allowedPaths.contains(this.getPathFromRoot())) {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "Illegal path \"%s\" for component type %s. Allowed paths: %s",
                this.getPathFromRoot(), this.getType().getKey(), allowedPaths.stream().map(HierarchyPath::toLiteral).reduce("", (s1, s2) -> s1 + ", " + s2)));
        }
    }

}