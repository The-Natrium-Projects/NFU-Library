package net.sodiumzh.nfu.entity.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.INBTSerializable;
import net.sodiumzh.nfu.annotation.DontOverride;
import net.sodiumzh.nfu.object.HierarchyPath;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

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
 * <b>Terms:</b>
 * <ul>
 *     <li>Sub-component: a component as a <b>direct child node</b> of self in the hierarchy tree.</li>
 *     <li>Parent component: a component as <b>the direct parent node</b> of self in the hierarchy tree.</li>
 *     <li>Downstream component: any component as a <b>child, grandchild... node</b> of self in the hierarchy tree.</li>
 *     <li>Upstream component: any component as <b>parent, grandparent... nodes</b> of self in the hierarchy tree. </li>
 * </ul>
 * <b>Best practice is to extend {@link EntityComponentBase} for field-backed implementations unless a very specialized structure is needed.</b>
 * @param <E> Required base entity class for this component. Note that a component can only be
 *           attached to entities whose class extends this component's entity class. There is no
 *           restriction on the entity class of its sub-components.
 */
public interface IEntityComponent<E extends Entity> extends INBTSerializable<CompoundTag> {

    /**
     * Gets the parent component of this component in the tree.
     * @return Optional parent component, or empty if this is the root, or if not yet attached.
     */
    Optional<IEntityComponent<?>> getParent();

    /**
     * Gets the entity's base class requirement to use this component. Note that a component
     * can only be attached to an entity whose class extends this component's entity class.
     */
    default public Class<? extends E> getRequiredEntityClass() {
        return this.getType().entityClass();
    }

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
    void updateParent(@Nullable IEntityComponent<?> oldParent, @Nullable IEntityComponent<?> newParent);


    /**
     * Sets the parent component and name for this component. Should be called by the manager when attaching or rebuilding the tree.
     * When attaching, this component must have no parent, or it throws. To change the parent, call detachFromParent first.
     * @param parent The parent component, or null if making root/unattached.
     * @param name The local subcomponent name (unique within parent), or null if root or.
     */
    @DontOverride
    default void attachTo(@Nullable IEntityComponent<?> parent, String name) {
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
    @Nonnull
    E getEntity();

    // PATH-BASED OPERATIONS

    /**
     * Get the full path from the root of component tree this component belongs to.
     */
    HierarchyPath getPathFromRoot();

    /**
     * Get the relative path from a given upstream component.
     * @return relative path from the upstream component to {@code this}. Empty string (i.e. {@code Optional.of(ComponentPath.empty())})
     * if input == this. {@link Optional#empty()} if the input is not an upstream component.
     */
    Optional<HierarchyPath> getPathFrom(IEntityComponent<?> upstreamComponent);

    /**
     * Get the relative path from {@code this} to a given downstream component.
     * @return relative path from {@code this} to the downstream component. Empty string (i.e. {@code Optional.of(ComponentPath.empty())})
     * if input == this. {@link Optional#empty()} if the input is not a downstream component.
     */
    default Optional<HierarchyPath> getPathTo(IEntityComponent<? extends Entity> downstreamComponent) {
        return downstreamComponent.getPathFrom(this);
    }

    // SUB-COMPONENT GETTERS

    /**
     * Gets an unmodifiable map of all direct subcomponents keyed by their local name.
     */
    Map<String, IEntityComponent<? extends Entity>> getSubComponents();

    /**
     * Gets a direct subcomponent by name.
     * @param name The local name of the subcomponent; may be empty.
     * @return The subcomponent or empty if not found.
     */
    Optional<IEntityComponent<? extends Entity>> getSubComponent(String name);

    /**
     * Gets a direct subcomponent by name.
     * @param name The local name of the subcomponent; may be empty.
     * @return The subcomponent. Empty if not found or type mismatch.
     */
    @SuppressWarnings("unchecked")
    default <C extends IEntityComponent<? extends Entity>> Optional<C> getSubComponent(String name, EntityComponentType<? extends Entity, C> type) {
        return this.getSubComponent(name).filter(c -> type.equals(c.getType())).map(c -> (C)c);
    }

    /**
     * Get the name if the input component is a direct sub-component of this component. Empty if it's not.
     */
    Optional<String> getSubComponentName(@Nonnull IEntityComponent<? extends Entity> subComponent);

    /**
     * Returns a set of all downstream (subtree) components into a given set: all direct and indirect (recursive) subcomponents.
     * Each upstream component is guaranteed to appear before any of its own downstream components
     * (i.e., preorder traversal). Order between branches is not defined. Not including self.
     * @param outSet Set to put components in. It will NOT be cleared before addition to allow recursion without
     *               creating multiple sets.
     */
    @ApiStatus.OverrideOnly
    void collectDownstreamComponentsTo(@Nonnull HashSet<IEntityComponent> outSet);

    /**
     * Returns a set of all downstream (subtree) components: all direct and indirect (recursive) subcomponents.
     * Each upstream component is guaranteed to appear before any of its own downstream components
     * (i.e., preorder traversal). Order between branches is not defined. Not including self.
     */
    default Set<IEntityComponent<? extends Entity>> getDownstreamComponents() {
        HashSet<IEntityComponent> res = new HashSet<>();
        collectDownstreamComponentsTo(res);
        return res.stream().map(c -> (IEntityComponent<? extends Entity>)c).collect(Collectors.toSet());
    }

    /**
     * Returns a list of self and all downstream (subtree) components: all direct and indirect (recursive) subcomponents.
     * Each upstream component is guaranteed to appear before any of its own downstream components
     * (i.e., preorder traversal). Order between branches is not defined.
     */
    default Set<IEntityComponent<? extends Entity>> getSelfAndDownstreamComponents() {
        HashSet<IEntityComponent> res = new HashSet<>();
        collectDownstreamComponentsTo(res);
        res.add(this);
        return res.stream().map(c -> (IEntityComponent<? extends Entity>)c).collect(Collectors.toSet());
    }

    /**
     * Get a component from this component's downstream tree with its path.
     * @param path Sub-component path. Example: {@code "/some/sub/component"} means:
     *             {@code this.getSubComponent("some").flatMap(c -> c.getSubComponent("sub").
     *             flatMap(c -> c.getSubComponent("component"))}.
     * @return The component, or empty if the component is missing.
     */
    Optional<IEntityComponent<? extends Entity>> getSubComponentByPath(HierarchyPath path);

    /**
     * Get a component from this component's downstream tree with its path and type.
     * @param path Sub-component path. Example: {@code "/some/sub/component"} means:
     *             {@code this.getSubComponent("some").flatMap(c -> c.getSubComponent("sub").
     *             flatMap(c -> c.getSubComponent("component"))}.
     * @return The component, or empty if the component is missing / type mismatching.
     */
    @SuppressWarnings("unchecked")
    default <C extends IEntityComponent<? extends Entity>> Optional<C> getSubComponentByPath(HierarchyPath path, EntityComponentType<? extends Entity, C> type) {
        return this.getSubComponentByPath(path).filter(c -> c.getType().equals(type)).map(c -> (C)c);
    }

    /**
     * Get a mapping of all components with its
     */
    default <T extends IEntityComponent<? extends Entity>> Map<HierarchyPath, T> getSubComponentsByType(EntityComponentType<?, T> type) {
        return this.getSubComponents().values().stream().filter(c -> c.getType().equals(type))
            .collect(Collectors.toMap(IEntityComponent::getPathFromRoot, c -> (T)c));
    }

    // SUB-COMPONENT SETTERS

    /**
     * Adds a subcomponent (child node) to this component. Enforces no cycles.
     * The subcomponent cannot have an existing parent, or it throws. Detach first.
     * The name must not be present, or it throws. To replace, call replaceSubComponent instead.
     */
    void addSubComponent(@Nonnull String name, @Nonnull IEntityComponent<? extends Entity> component);

    /**
     * Add an indirect sub-component by its relevant path from this component.
     * @param fillNodesIfParentAbsent If true, if an upstream component of the given path is absent, it will fill with node component ({@link EntityNodeComponent}).
     *                                Otherwise it throws in this case. Set this true only when you're sure its upstream nodes must be nodes.
     */
    void addSubComponentByPath(HierarchyPath path, IEntityComponent<? extends Entity> component, boolean fillNodesIfParentAbsent);


    default void addSubComponentByPath(HierarchyPath path, IEntityComponent<? extends Entity> component) {
        this.addSubComponentByPath(path, component, false);
    }

    /**
     * Removes a subcomponent (child node) from this component by its name.
     * @param name The name of the component to remove.
     * @return the removed component, or null if not removed.
     */
    @Nullable
    IEntityComponent<? extends Entity> removeSubComponent(String name);

    @DontOverride
    default void removeSubComponent(IEntityComponent<? extends Entity> component) {
        this.getSubComponents().entrySet().stream().filter(e -> e.getValue() == component)
            .toList().forEach(e -> this.removeSubComponent(e.getKey()));
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
     * Replace the existing subcomponent as the new one. Returns the old one (absent = return null).
     * @return The old component.
     */
    @Nullable
    @DontOverride
    default IEntityComponent<? extends Entity> replaceSubComponent(@Nonnull String name, @Nullable IEntityComponent<? extends Entity> newComponent) {
        IEntityComponent<? extends Entity> res = this.removeSubComponent(name);
        if (newComponent == null) return res;
        this.addSubComponent(name, newComponent);
        return res;
    }

    // MISC

    /**
     * Called by the component manager when the entity is ticked.
     */
    void tick();

    /**
     * Get the component type of this component. Note tha the component type must be registered
     * in {@link net.sodiumzh.nfu.registry.NFURegistries#ENTITY_COMPONENT_TYPES}, or it will cause an exception.
     */
    @Nonnull EntityComponentType<E, ? extends IEntityComponent<E>> getType();

    /**
     * Set the component type.
     */
    @ApiStatus.OverrideOnly
    void setType(EntityComponentType<E, ? extends IEntityComponent<E>> type);

    /**
     * Get the root of the component tree this component belongs to.
     */
    IEntityComponent<?> getRoot();

    /**
     * Get the manager (root) if this component belongs to a component tree rooted by a component manager.
     * Empty if not found.
     */
    default Optional<CEntityComponentManager> getComponentManager() {
        return (this.getRoot() instanceof CEntityComponentManager mgr) ? Optional.of(mgr) : Optional.empty();
    }

    @DontOverride
    default boolean isClientSide() {
        return this.getEntity().level.isClientSide();
    }

    // INBTSerializable<CompoundTag> methods:
    // CompoundTag serializeNBT();
    // void deserializeNBT(CompoundTag nbt);

    boolean shouldSerialize();

    void setSerialize(boolean shouldSerialize);

    /**
     * Check if this component is in use and should be ticked.
     * <p>If a component's ticking may cause resource waste when unused, override this method and return the condition of ticking.
     */
    default boolean shouldTick() {
        return true;
    }

    /**
     * Create saved data. Serialization here can return null safely if nothing should be saved.
     */
    @Nullable CompoundTag serializeNBT();

    /**
     * Quick check if this component's path in the component manager equals the given path.
     * <p>This operation is quicker than {@code getPathFromRoot}
     */
    default boolean pathInManagerEquals(HierarchyPath path) {
        return EntityComponentAPI.getComponentByPath(this.getEntity(), path).filter(c -> c.equals(this)).isPresent();
    }

}