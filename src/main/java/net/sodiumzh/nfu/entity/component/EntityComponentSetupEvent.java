package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.event.NFUEntityEvent;
import net.sodiumzh.nfu.object.HierarchyPath;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

public class EntityComponentSetupEvent extends NFUEntityEvent<Entity> {

    private final CEntityComponentManager manager;
    private final Map<HierarchyPath, Tuple2<EntityComponentType<? extends Entity, ?>, PreConstructPriority>> componentTypesAndPriorities = new HashMap<>();
    // Transient map, shared from CEntityComponentManagerImpl. Updating this map will update the component manager at the same time
    private Map<HierarchyPath, IEntityComponent<?>> preConstructed;

    public EntityComponentSetupEvent(Entity entity, CEntityComponentManager manager) {
        super(entity);
        this.manager = manager;
    }

    /**
     * Add a component.
     * <p>Note: this method doesn't add component immediately. Component info will be added, and components will be added altogether
     * after the event.
     * <p>Note: If a component's constructor involves references to other components, then the latter must be pre-constructed, and its pre-construction priority
     * must be higher than the former. During construction, only {@link CEntityComponentManager#getSubComponent}, {@link CEntityComponentManager#getSubComponents},
     * {@link CEntityComponentManager#getSubComponentsByType} and {@link CEntityComponentManager#getSubComponentByPath} are reliable.
     * @param preConstructPriority Some components may need to be constructed in advance as other components depend on them on construction.
     *                             If a component needs to be pre-constructed, use this parameter to define which component should be constructed earlier.
     *                             Leave it {@code NONE} to disable pre-construction (default, omit-able).
     */
    public void addComponent(HierarchyPath path, EntityComponentType<? extends Entity, ?> type, PreConstructPriority preConstructPriority) {
        Tuple2<EntityComponentType<? extends Entity, ?>, PreConstructPriority> entry = null;
        // Duplication handling
        if (componentTypesAndPriorities.containsKey(path)) {
            // Node doesn't overwrite anything, ignore
            if (type.equals(EntityComponentTypes.NODE.get())) {}
            // Other components overwrite node
            else if (componentTypesAndPriorities.get(path).getA().equals(EntityComponentTypes.NODE.get()))
                entry = new Tuple2<>(type, preConstructPriority);
            // Occupied with same type, no conflict, ignore
            else if (type.equals(componentTypesAndPriorities.get(path).getA())) {}
            // Of different types, and both not node, conflict, throw
            else throw new IllegalArgumentException("Duplicate key " + path);
        }
        else entry = new Tuple2<>(type, preConstructPriority);

        if (entry == null) return;
        if (preConstructPriority.shouldConstructImmediately()) {
            this.preConstructed.put(path, type.createUnsafe(this.getEntity()));  // preConstructed directly mirrors CEntityComponentManagerImpl#preConstructed
        }
        this.componentTypesAndPriorities.put(path, entry);
    }

    /**
     * Add a component.
     * <p>Note: this method doesn't add component immediately. Component info will be added, and components will be added altogether
     * after the event.
     * <p>Note: If a component's constructor involves references to other components, then the latter must be pre-constructed, and its pre-construction priority
     * must be higher than the former. During construction, only {@link CEntityComponentManager#getSubComponent}, {@link CEntityComponentManager#getSubComponents},
     * {@link CEntityComponentManager#getSubComponentsByType} and {@link CEntityComponentManager#getSubComponentByPath} are reliable.
     * @param preConstructPriority Some components may need to be constructed in advance as other components depend on them on construction.
     *                             If a component needs to be pre-constructed, use this parameter to define which component should be constructed earlier.
     *                             Leave it {@code NONE} to disable pre-construction (default, omit-able).
     */
    public void addComponent(String path, EntityComponentType<? extends Entity, ?> type, PreConstructPriority preConstructPriority) {
        this.addComponent(HierarchyPath.byLiteral(path), type, preConstructPriority);
    }

    /**
     * Add a component without pre-construction.
     * <p>Note: this method doesn't add component immediately. Component info will be added, and components will be added altogether
     * after the event.
     * <p>Note: If a component's constructor involves references to other components, then the latter must be pre-constructed, and its pre-construction priority
     * must be higher than the former. During construction, only {@link CEntityComponentManager#getSubComponent}, {@link CEntityComponentManager#getSubComponents},
     * {@link CEntityComponentManager#getSubComponentsByType} and {@link CEntityComponentManager#getSubComponentByPath} are reliable.
     */
    public void addComponent(HierarchyPath path, EntityComponentType<? extends Entity, ?> type) {
        this.addComponent(path, type, PreConstructPriority.NONE);
    }

    /**
     * Add a node at given path. A node is a component working only as a node of the component hierarchy, but don't do anything itself.
     */
    public void addNode(HierarchyPath path) {
        addComponent(path, EntityComponentTypes.NODE.get(), PreConstructPriority.NONE);
    }

    /**
     * Add a component at given path, and fill its upstream paths with nodes if absent.
     * <p>Example: {@code addComponentAndNodes("/a/b/c", type)} will add a component of given type
     * at {@code "/a/b/c"}, and add nodes at {@code "/a"} and {@code "/a/b"}. If an upstream node is already
     * occupied, it will be ignored.
     */
    public void addComponentAndUpstreamNodes(HierarchyPath path, EntityComponentType<? extends Entity, ?> type) {
        String[] split = path.toStringArray();
        for (int i = 0; i < split.length - 1; ++i) {
            this.addNode(HierarchyPath.byNameArray(Arrays.copyOf(split, i + 1)));
        }
        this.addComponent(path, type);
    }

    /**
     * Only called in entity component manager, share the pre-constructed map to event instance
     * @param map Map reference (not copy)
     */
    void sharePreConstructedMapFrom(Map<HierarchyPath, IEntityComponent<?>> map) {
        this.preConstructed = map;
    }

    /**
     * Do pre-construction and return the pre-constructed map to component manager to update its temporal reference map.
     */
    @ApiStatus.Internal
    Map<HierarchyPath, IEntityComponent<?>> preConstruct() {
        componentTypesAndPriorities.entrySet().stream()
            .filter(entry -> entry.getValue().getB().shouldConstructInPreConstructionPhase())
            .sorted((e1, e2) -> {
                int priorityDiff = e1.getValue().getB().id() - e2.getValue().getB().id();
                if (priorityDiff != 0) return priorityDiff;
                else if (e1.getKey().equals(e2.getKey())) throw new RuntimeException();
                else if (e1.getKey().isUpstreamOf(e2.getKey())) return -1;
                else if (e1.getKey().isDownstreamOf(e2.getKey())) return 1;
                else return 0;
            }).forEach(entry -> this.preConstructed.put(entry.getKey(), entry.getValue().getA().createUnsafe(this.getEntity())));
        return preConstructed;
    }

    /**
     * Finally construct the component tree.
     */
    @ApiStatus.Internal
    void collect() {
        componentTypesAndPriorities.entrySet().stream().sorted((e1, e2) -> {
            if (e1.getKey().equals(e2.getKey())) return 0;
            else if (e1.getKey().isUpstreamOf(e2.getKey())) return -1;
            else if (e1.getKey().isDownstreamOf(e2.getKey())) return 1;
            else return 0;
        }).forEach(e -> {
            IEntityComponent<?> component = preConstructed.get(e.getKey());
            if (component == null) component = e.getValue().getA().createUnsafe(this.getEntity());
            this.manager.addSubComponentByPath(e.getKey(), component);
        });
    }

    @ApiStatus.Internal
    void checkHierarchy() {
        this.manager.getDownstreamComponents().stream()
            .filter(c -> c instanceof EntityComponentBase<? extends Entity>).map(c -> (EntityComponentBase<? extends Entity>)c)
            .forEach(EntityComponentBase::checkHierarchy);
    }

    public static enum PreConstructPriority {
        /**
         * Construct the component immediately on calling {@link EntityComponentSetupEvent#addComponent}.
         * Used only when you expect some listeners of {@link EntityComponentSetupEvent} itself
         * may depend on this component. Its ordering depends on the calling order in event listener methods.
         * <p>e.g. If you need to add component B only when component A meets a certain requirement, then you'll want to construct A immediately.
         * After this, you can access component A by {@link EntityComponentAPI#getComponentByPath}.
         */
        IMMEDIATE(-1),
        HIGHEST(0),
        HIGH(1),
        NORMAL(2),
        LOW(3),
        LOWEST(4),
        /**
         * No pre-construction, default
         */
        NONE(5);

        private final int id;

        PreConstructPriority(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public boolean shouldConstructInPreConstructionPhase() {
            return this != IMMEDIATE && this != NONE;
        }

        public boolean shouldConstructImmediately() {
            return this == IMMEDIATE;
        }

        public boolean shouldConstructNormally() {
            return this == NONE;
        }
    }
}
