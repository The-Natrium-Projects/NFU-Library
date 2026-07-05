package net.sodiumzh.nfu.entity.component;

import com.mojang.blaze3d.shaders.Effect;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.event.NFUEntityEvent;
import net.sodiumzh.nfu.exception.WrongSideException;
import net.sodiumzh.nfu.network.AvailableSide;
import net.sodiumzh.nfu.object.HierarchyPath;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

public class EntityComponentSetupEvent extends NFUEntityEvent<Entity> {

    private final CEntityComponentManager manager;
    private final Map<HierarchyPath, ComponentConstructionInfo> componentTypesAndPriorities = new HashMap<>();
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
     *                             Leave it {@code NONE} to disable pre-construction (default, omit-able). If you need pre-construction, use priority NORMAL
     *                             unless you have a specific reason for another.
     * @param side Specify which side this component should be added. If there isn't another component added in another side, a node will be added to keep
     *             the hierarchy. If the
     */
    public void addComponent(HierarchyPath path, EntityComponentType<? extends Entity, ?> type, PreConstructPriority preConstructPriority, AvailableSide side) {
        ComponentConstructionInfo info = null;
        // Check valid side setting
        if (side == AvailableSide.SERVER && type.availableSide() == AvailableSide.CLIENT)
            throw new WrongSideException(String.format("Adding client-only component %s on server.", type.getKey().toString()));
        if (side == AvailableSide.CLIENT && type.availableSide() == AvailableSide.SERVER)
            throw new WrongSideException(String.format("Adding server-only component %s on client.", type.getKey().toString()));
        AvailableSide actualSide = type.availableSide() == AvailableSide.BOTH ? side : type.availableSide();
        EntityComponentType<? extends Entity, ?> actualType = actualSide.isCorrectSide(this.manager.getEntity()) ? type : EntityComponentTypes.NODE.get();

        // Duplication handling
        if (componentTypesAndPriorities.containsKey(path)) {
            // Node doesn't overwrite anything, ignore
            if (actualType.equals(EntityComponentTypes.NODE.get())) {}
            // Other components overwrite node
            else if (componentTypesAndPriorities.get(path).type().equals(EntityComponentTypes.NODE.get()))
                info = new ComponentConstructionInfo(actualType, preConstructPriority);
            // Occupied with same type, no conflict, ignore
            else if (actualType.equals(componentTypesAndPriorities.get(path).type())) {}
            // Of different types, and both not node, conflict, throw
            else throw new IllegalArgumentException("Duplicate key " + path);
        }
        else info = new ComponentConstructionInfo(actualType, preConstructPriority);

        if (info == null) return;
        if (preConstructPriority.shouldConstructImmediately()) {
            this.preConstructed.put(path, type.createUnsafe(this.getEntity()));  // preConstructed directly mirrors CEntityComponentManagerImpl#preConstructed
        }
        this.componentTypesAndPriorities.put(path, info);
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
     *                             Leave it {@code NONE} to disable pre-construction (default, omit-able). If you need pre-construction, use priority NORMAL
     *                             unless you have a specific reason for another.
     * @param side Specify which side this component should be added. If there isn't another component added in another side, a node will be added to keep
     *             the hierarchy.
     */
    public void addComponent(String path, EntityComponentType<? extends Entity, ?> type, PreConstructPriority preConstructPriority, AvailableSide side) {
        this.addComponent(HierarchyPath.byLiteral(path), type, preConstructPriority, side);
    }

    /**
     * Add a component to both sides.
     * <p>Note: this method doesn't add component immediately. Component info will be added, and components will be added altogether
     * after the event.
     * <p>Note: If a component's constructor involves references to other components, then the latter must be pre-constructed, and its pre-construction priority
     * must be higher than the former. During construction, only {@link CEntityComponentManager#getSubComponent}, {@link CEntityComponentManager#getSubComponents},
     * {@link CEntityComponentManager#getSubComponentsByType} and {@link CEntityComponentManager#getSubComponentByPath} are reliable.
     * @param preConstructPriority Some components may need to be constructed in advance as other components depend on them on construction.
     *                             If a component needs to be pre-constructed, use this parameter to define which component should be constructed earlier.
     *                             Leave it {@code NONE} to disable pre-construction (default, omit-able). If you need pre-construction, use priority NORMAL
     *                             unless you have a specific reason for another.
     */
    public void addComponent(HierarchyPath path, EntityComponentType<? extends Entity, ?> type, PreConstructPriority preConstructPriority) {
        this.addComponent(path, type, preConstructPriority, AvailableSide.BOTH);
    }

    /**
     * Add a component without pre-construction.
     * <p>Note: this method doesn't add component immediately. Component info will be added, and components will be added altogether
     * after the event.
     * <p>Note: If a component's constructor involves references to other components, then the latter must be pre-constructed, and its pre-construction priority
     * must be higher than the former. During construction, only {@link CEntityComponentManager#getSubComponent}, {@link CEntityComponentManager#getSubComponents},
     * {@link CEntityComponentManager#getSubComponentsByType} and {@link CEntityComponentManager#getSubComponentByPath} are reliable.
     * @param side Specify which side this component should be added. If there isn't another component added in another side, a node will be added to keep
     *             the hierarchy.
     */
    public void addComponent(HierarchyPath path, EntityComponentType<? extends Entity, ?> type, AvailableSide side) {
        this.addComponent(path, type, PreConstructPriority.NONE, side);
    }

    /**
     * Add a component to both sides.
     * <p>Note: this method doesn't add component immediately. Component info will be added, and components will be added altogether
     * after the event.
     * <p>Note: If a component's constructor involves references to other components, then the latter must be pre-constructed, and its pre-construction priority
     * must be higher than the former. During construction, only {@link CEntityComponentManager#getSubComponent}, {@link CEntityComponentManager#getSubComponents},
     * {@link CEntityComponentManager#getSubComponentsByType} and {@link CEntityComponentManager#getSubComponentByPath} are reliable.
     * @param preConstructPriority Some components may need to be constructed in advance as other components depend on them on construction.
     *                             If a component needs to be pre-constructed, use this parameter to define which component should be constructed earlier.
     *                             Leave it {@code NONE} to disable pre-construction (default, omit-able). If you need pre-construction, use priority NORMAL
     *                              unless you have a specific reason for another.
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
     * @param side Specify which side this component should be added. If there isn't another component added in another side, a node will be added to keep
     *             the hierarchy.
     */
    public void addComponent(String path, EntityComponentType<? extends Entity, ?> type, AvailableSide side) {
        this.addComponent(HierarchyPath.byLiteral(path), type, PreConstructPriority.NONE, side);
    }

    /**
     * Add a component to both sides without pre-construction.
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
     * Add a component to both sides without pre-construction.
     * <p>Note: this method doesn't add component immediately. Component info will be added, and components will be added altogether
     * after the event.
     * <p>Note: If a component's constructor involves references to other components, then the latter must be pre-constructed, and its pre-construction priority
     * must be higher than the former. During construction, only {@link CEntityComponentManager#getSubComponent}, {@link CEntityComponentManager#getSubComponents},
     * {@link CEntityComponentManager#getSubComponentsByType} and {@link CEntityComponentManager#getSubComponentByPath} are reliable.
     */
    public void addComponent(String path, EntityComponentType<? extends Entity, ?> type) {
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
    public void addComponentAndUpstreamNodes(HierarchyPath path, EntityComponentType<? extends Entity, ?> type, PreConstructPriority priority, AvailableSide side) {
        String[] split = path.toStringArray();
        for (int i = 0; i < split.length - 1; ++i) {
            this.addNode(HierarchyPath.byNameArray(Arrays.copyOf(split, i + 1)));
        }
        this.addComponent(path, type, priority, side);
    }

    /**
     * Add a component at given path, and fill its upstream paths with nodes if absent. To both sides without pre-construction.
     * <p>Example: {@code addComponentAndNodes("/a/b/c", type)} will add a component of given type
     * at {@code "/a/b/c"}, and add nodes at {@code "/a"} and {@code "/a/b"}. If an upstream node is already
     * occupied, it will be ignored.
     */
    public void addComponentAndUpstreamNodes(HierarchyPath path, EntityComponentType<? extends Entity, ?> type) {
        this.addComponentAndUpstreamNodes(path, type, PreConstructPriority.NONE, AvailableSide.BOTH);
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
            .filter(entry -> entry.getValue().priority().shouldConstructInPreConstructionPhase())
            // Practically path depth is impossible to get 1e+8; this is for comparison priority
            // Compare path length to ensure "upstream before downstream", as downstream is always longer than upstream
            .sorted(Comparator.comparingInt(entry -> entry.getValue().priority().id() * 100000000 + entry.getKey().length()))
            .forEach(entry -> this.preConstructed.put(entry.getKey(), entry.getValue().type().createUnsafe(this.getEntity())));
        return preConstructed;
    }

    /**
     * Finally construct the component tree.
     */
    @ApiStatus.Internal
    void collect() {
        componentTypesAndPriorities.entrySet().stream()
            // Compare path length to ensure "upstream before downstream", as downstream is always longer than upstream
            .sorted(Comparator.comparingInt(entry -> entry.getKey().length()))
            .forEach(e -> {
                IEntityComponent<?> component = preConstructed.get(e.getKey());
                if (component == null) component = e.getValue().type().createUnsafe(this.getEntity());
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

    private static record ComponentConstructionInfo(EntityComponentType<? extends Entity, ?> type, PreConstructPriority priority){};

}
