package net.sodiumzh.nfu.entity.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.INBTSerializable;
import net.sodiumzh.nfu.annotation.DontCallManually;
import net.sodiumzh.nfu.annotation.DontOverride;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * The core interface representing a node in the entity component tree.
 * <p>
 * {@code IEntityComponent} defines the behavioral contract for hierarchical component systems on entities,
 * facilitating not just modular data storage but also runtime navigation, capability bridging, and requirement verification.
 * <p>
 * <b>Core features:</b>
 * <ul>
 *   <li>Supports stable parent-child relationships, self-recognizing location and ancestry in the tree.</li>
 *   <li>Allows both direct and path-based navigation (including convenience helpers for upstream, downstream, and cross-tree traversal).</li>
 *   <li>Encapsulates design for required subcomponent enforcement, using normalized paths and explicit type guarantees.</li>
 *   <li>Default methods are provided for most navigation, path management, and convenience contract operations; implementations only need to handle state.</li>
 *   <li>Works seamlessly with Forge's NBT serialization and capability APIs for snapshotting and restoring stateful data.</li>
 * </ul>
 * <b>Best practice is to extend {@link EntityComponentBase} for field-backed implementations unless a very specialized structure is needed.</b>
 * @param <T> Required base entity class for this component. Note that a component can only be attached to parents with the 
 *           entity class supers this component's entity class.
 */
public interface IEntityComponent<T extends Entity> extends INBTSerializable<CompoundTag> {

    /**
     * Gets the parent component of this component in the tree.
     * @return Optional parent component, or empty if this is the root, or if not yet attached.
     */
    Optional<IEntityComponent<? super T>> getParent();

    /**
     * Gets the entity's base class requirement to use this component. Note that a component
     * can only be attached to a parent supers its entity class.
     */
    public Class<T> getEntityClass();
    
    /**
     * Gets the local name (string identifier) of this component within its parent, or empty if root.
     * @return local component name (unique in parent)
     */
    @DontOverride
    default Optional<String> getNameInParent() {
        return this.getParent().flatMap(p -> p.getSubComponentName(this));
    }

    /**
     * Update when the parent changes e.g. updating the parent reference field. It should only be called in
     * parent-changing actions (e.g. {@code attachTo}, {@code addSubComponent}), and not be called elsewhere.
     */
    @DontCallManually
    default void updateParent(@Nullable IEntityComponent<? super T> oldParent, @Nullable IEntityComponent<? super T> newParent) {}

    /**
     * Sets the parent component and name for this component. Should be called by the manager when attaching or rebuilding the tree.
     * When attaching, this component must have no parent, or it throws. To change the parent, call detachFromParent first.
     * @param parent The parent component, or null if making root/unattached.
     * @param name The local subcomponent name (unique within parent), or null if root or.
     */
    @DontOverride
    default void attachTo(@Nullable IEntityComponent<? super T> parent, String name) {
        if (parent == null) {
            this.detachFromParent();
            return;
        }
        if (this.getParent().isPresent())
            throw new IllegalStateException("Calling attachTo when parent is present. Detach first");
        parent.addSubComponent(name, this);
    }

    @DontOverride
    default void detachFromParent() {
        this.getParent().ifPresent(parent -> parent.removeSubComponent(this));
    }

    /**
     * Returns the entity this component is attached to. Not nullable.
     */
    Entity getEntity();

    /**
     * Adds a subcomponent (child node) to this component. Enforces no cycles.
     * The subcomponent cannot have an existing parent, or it throws. Detach first.
     * The name must not be present, or it throws. To replace, call replaceSubComponent instead.
     */
    void addSubComponent(@Nonnull String name, @Nonnull IEntityComponent<? extends T> component);

    /**
     * Replace the existing subcomponent as the new one. Returns the old one (absent = return null).
     * @return The old component.
     */
    @Nullable
    @DontOverride
    default IEntityComponent<? extends T> replaceSubComponent(@Nonnull String name, @Nullable IEntityComponent<? extends T> newComponent) {
        IEntityComponent<? extends T> res = this.removeSubComponent(name);
        if (newComponent == null) return res;
        this.addSubComponent(name, newComponent);
        return res;
    }

    /**
     * Removes a subcomponent (child node) from this component by its name.
     * @param name The name of the component to remove.
     * @return the removed component, or null if not removed.
     */
    @Nullable
    IEntityComponent<? extends T> removeSubComponent(String name);

    @DontOverride
    default void removeSubComponent(IEntityComponent<? extends T> component) {
        this.getSubComponents().entrySet().stream().filter(e -> e.getValue() == component)
            .toList().forEach(e -> this.removeSubComponent(e.getKey()));
    }

    /**
     * Gets an unmodifiable map of all direct subcomponents keyed by their local name.
     */
    Map<String, IEntityComponent<? extends T>> getSubComponents();

    /**
     * Gets a direct subcomponent by name.
     * @param name The local name of the subcomponent; may be null.
     * @return The subcomponent or empty if not found.
     */
    @DontOverride
    default Optional<IEntityComponent<? extends T>> getSubComponent(String name) {
        return Optional.ofNullable(this.getSubComponents().get(name));
    }

    /**
     * Gets a direct subcomponent by name.
     * @param name The local name of the subcomponent; may be null.
     * @return The subcomponent. Empty if not found or type mismatch.
     */
    @SuppressWarnings("unchecked")
    default <C extends IEntityComponent<? extends T>> Optional<C> getSubComponent(String name, EntityComponentType<C> type) {
        return this.getSubComponent(name).filter(c -> c.getType().equals(type)).map(c -> (C)c);
    }

    /**
     * Get the name if the input component is a direct sub-component of this component. Empty if it's not.
     */
    @DontOverride
    default Optional<String> getSubComponentName(@Nonnull IEntityComponent<? extends T> subComponent) {
        return this.getSubComponents().entrySet().stream().filter(e -> e.getValue() == subComponent).findAny().map(Map.Entry::getKey);
    }

    /**
     * Remove all sub-components.
     * <p>Note: this action will only detach each sub-tree derived from each sub-component.
     * The sub-tree structures will not be broken.
     */
    @DontOverride
    default void clearSubComponents() {
        this.getSubComponents().keySet().stream().toList().forEach(this::removeSubComponent);
    }

    /**
     * Returns a list of all downstream components: all direct and indirect (recursive) subcomponents.
     * Each upstream component is guaranteed to appear before any of its own downstream components
     * (i.e., preorder traversal). Order between branches is not defined. Not including self.
     */
    @DontOverride
    default List<IEntityComponent<? extends T>> getAllDownstreamComponents() {
        List<IEntityComponent<? extends T>> result = new ArrayList<>();
        for (IEntityComponent<? extends T> child : getSubComponents().values()) {
            result.add(child);
            result.addAll(child.getAllDownstreamComponents());
        }
        return result;
    }

    /**
     * Called by the component manager when the entity is ticked.
     */
    void tick();

    /**
     * Get the component type of this component. Note tha the component type must be registered
     * in {@link net.sodiumzh.nfu.registry.NFURegistries#ENTITY_COMPONENT_TYPES}, or it will cause an exception.
     */
    EntityComponentType<?> getType();

    /**
     * Get a component from this component's downstream tree with its path.
     * @param path Sub-component path. Example: {@code "/some/sub/component"} means:
     *             {@code this.getSubComponent("some").flatMap(c -> c.getSubComponent("sub").
     *             flatMap(c -> c.getSubComponent("component"))}.
     * @return The component, or empty if the component is missing.
     */
    default Optional<IEntityComponent<? extends T>> getSubComponentByPath(String path) {
        List<String> parts = Arrays.stream(path.split("[/\\\\]+"))
            .filter(str -> !str.isEmpty()).toList();
        Optional<IEntityComponent<? extends T>> res = Optional.of(this);
        for (String name: parts) {
            res = res.flatMap(c -> c.getSubComponent(name));
        }
        return res;
    }

    /**
     * Get a component from this component's downstream tree with its path and type.
     * @param path Sub-component path. Example: {@code "/some/sub/component"} means:
     *             {@code this.getSubComponent("some").flatMap(c -> c.getSubComponent("sub").
     *             flatMap(c -> c.getSubComponent("component"))}.
     * @return The component, or empty if the component is missing / type mismatching.
     */
    @SuppressWarnings("unchecked")
    default <C extends IEntityComponent<? extends T>> Optional<C> getSubComponentByPath(String path, EntityComponentType<C> type) {
        List<String> parts = Arrays.stream(path.split("[/\\\\]+"))
            .filter(str -> !str.isEmpty()).toList();
        Optional<IEntityComponent<? extends T>> res = Optional.of(this);
        for (String name: parts) {
            res = res.flatMap(c -> c.getSubComponent(name));
        }
        return res.filter(c -> c.getType().equals(type)).map(c -> (C)c);
    }

    /**
     * Add an indirect sub-component by its relevant path from this component.
     * @throws 
     */
    default <U extends T> void addSubComponentByPath(String path, IEntityComponent<U> component) {
        List<String> parts = Arrays.stream(path.split("[/\\\\]+"))
            .filter(str -> !str.isEmpty()).toList();
        Optional<IEntityComponent<? extends T>> res = Optional.of(this);
        StringBuilder rebuiltPath = new StringBuilder();
        for (int i = 0; i < parts.size() - 1; ++i) {
            int j = i;
            if (res.isEmpty()) break;
            res = res.flatMap(c -> c.getSubComponent(parts.get(j)));
            rebuiltPath.append("\\").append(parts.get(j));
        }
        if (res.isEmpty()) {
            throw new IllegalStateException("Failed to add sub-component \"" + path
             + "\" because \"" + rebuiltPath + "\" is absent.");
        } else if (!res.orElseThrow().getEntityClass().isAssignableFrom(component.getEntityClass())) {
            throw new IllegalStateException("Failed to add sub-component \"" 
                    + path + "\" because its parent requires entity class " + res.orElseThrow().getEntityClass().getName()
            + "but the component is for " + component.getEntityClass().getName());
        } else {
            res.orElseThrow().addSubComponent(parts.get(parts.size() - 1), component);
        }
    }

    /**
     * Get the root of the component tree this component belongs to.
     */
    default IEntityComponent getRoot() {
        IEntityComponent ptr = this;
        while (ptr.getParent().isPresent()) {
            ptr = ptr.getParent().orElseThrow();
        }
        return ptr;
    }

    /**
     * Get the manager (root) if this component belongs to a component tree rooted by a component manager.
     * Empty if not found.
     */
    default Optional<CEntityComponentManager> getComponentManager() {
        return (this.getRoot() instanceof CEntityComponentManager mgr) ? Optional.of(mgr) : Optional.empty();
    }

    /**
     * Get the full path from the root of component tree this component belongs to.
     */
    default String getPathFromRoot() {
        IEntityComponent ptr = this;
        StringBuilder res = new StringBuilder();
        while (ptr.getParent().isPresent()) {
            ptr = ptr.getParent().orElseThrow();
            res.insert(0, "\\" + ptr.getNameInParent().orElseThrow());
        }
        return res.toString();
    }

    /**
     * Get the relative path from a given upstream component.
     * @return relative path from the upstream component to {@code this}. Empty string (i.e. {@code Optional.of("")})
     * if input == this. {@link Optional#empty()} if the input is not an upstream component.
     */
    default Optional<String> getPathFrom(IEntityComponent upstreamComponent) {
        IEntityComponent ptr = this;
        if (upstreamComponent == this) return Optional.of("");
        StringBuilder res = new StringBuilder();
        while (ptr.getParent().filter(c -> c != upstreamComponent).isPresent()) {
            ptr = ptr.getParent().orElseThrow();
            res.insert(0, "\\" + ptr.getNameInParent().orElseThrow());
        }
        return ptr.getParent().isPresent() ? Optional.of(res.toString()) : Optional.empty();
    }

    /**
     * Get the relative path from {@code this} to a given downstream component.
     * @return relative path from {@code this} to the downstream component. Empty string (i.e. {@code Optional.of("")})
     * if input == this. {@link Optional#empty()} if the input is not a downstream component.
     */
    default Optional<String> getPathTo(IEntityComponent downstreamComponent) {
        return downstreamComponent.getPathFrom(this);
    }

    /**
     * Declares that a sub-component with given type and path should always be present. If
     * the required sub-component is not present in the given position, it will be auto created
     * through factory.
     * The component manager will check the presence of all required components, and throw
     * if not found.
     * @throws IllegalStateException When the path is occupied by a component of wrong type,
     * or the parent component of the desired path is absent.
     */
    void setRequired(String path, EntityComponentType<?> type);

    /**
     * Check if a path has a required component. If yes, return an {@link Optional} of
     * the required component type, otherwise empty.
     * <p>Note: this method doesn't handle the presence check of the required component instances.
     * It's done in {@link CEntityComponentManager}.
     */
    Optional<EntityComponentType<?>> getTypeIfRequired(String path);

    /**
     * Get an immutable map of all required paths and corresponding types.
     */
    Map<String, EntityComponentType<?>> getAllRequired();

    @DontOverride
    default boolean isClientSide() {
        return this.getEntity().level().isClientSide;
    }

    // INBTSerializable<CompoundTag> methods:
    // CompoundTag serializeNBT();
    // void deserializeNBT(CompoundTag nbt);
}