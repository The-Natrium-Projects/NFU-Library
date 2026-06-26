package net.sodiumzh.nfu.entity.component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.annotation.DontCallManually;
import net.sodiumzh.nfu.capability.CEntityTickingCapability;
import net.sodiumzh.nfu.object.HierarchyPath;
import net.sodiumzh.nfu.registry.NFUEntityComponents;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Marker interface for entity component tree roots in the capability system.
 * <p>
 * {@code CEntityComponentManager} acts as the anchor for the entire component tree on a given entity, being attached
 * (typically via Forge's capability mechanism) exactly once per entity. It inherits all interface contracts for ticking,
 * tree navigation, and serialization from both {@link CEntityTickingCapability} and {@link IEntityComponent}.
 * <p>
 * Core behaviors:
 * <ul>
 *   <li>Acts as the root node; must never have a parent; is responsible for the lifetime and visibility of all descendants.</li>
 *   <li>Should be created and attached only during capability initialization, not by direct construction outside the registration context.</li>
 *   <li>Component managers are discoverable via capability lookups, and utility entrypoints exist to assist with safe retrieval.</li>
 *   <li>On newly created entities, static initialization logic may attach and populate a default component tree;
 *   on entity data load, the tree is reconstructed dynamically from saved data.</li>
 *   <li>Direct instantiation from type factory is blocked.</li>
 * </ul>
 * <b>Do not implement this interface manually; extend its core implementation to ensure validity and correct bookkeeping.</b>
 */
@ApiStatus.NonExtendable
public interface CEntityComponentManager extends CEntityTickingCapability<Entity>, IEntityComponent<Entity> {

    /**
     * Dummy factory for type registry.
     */
    @DontCallManually
    static CEntityComponentManager factory(Entity entity) {
        throw new IllegalArgumentException("CEntityComponentManager could not be created through factory.");
    }

    @Nonnull
    public static CEntityComponentManager getManager(Entity e) {
        return e.getCapability(EntityComponentStatics.CAP_MANAGER).orElse(new CEntityComponentManagerImpl(e));
    }

    /**
     * Print the full component tree structure in ASCII.
     * Each node is drawn as "{name}: {type_key}".
     * Indents and branches are formatted for clarity.
     */
    default void printComponentTree() {
        // Root node (this) itself may not have a "name", so we print its children at top level.
        Map<String, IEntityComponent<? extends Entity>> rootChildren = this.getSubComponents();
        int sz = rootChildren.size();
        int i = 0;
        for (Map.Entry<String, IEntityComponent<? extends Entity>> entry : rootChildren.entrySet()) {
            ++i;
            printComponentTreeRec(entry.getKey(), entry.getValue(), "", i == sz);
        }
    }

    // Recursive helper prints node and all descendants with proper ASCII structure
    private static void printComponentTreeRec(String name, IEntityComponent<? extends Entity> node, String indent, boolean last) {
        ResourceLocation typeKey = node.getType().getKey();
        System.out.println(indent + (last ? "└─" : "├─") + name + ": " + typeKey);
        Map<String, IEntityComponent<? extends Entity>> children = node.getSubComponents().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (IEntityComponent<? extends Entity>) (e.getValue())));
        int sz = children.size();
        int idx = 0;
        for (Map.Entry<String, IEntityComponent<? extends Entity>> entry : children.entrySet()) {
            ++idx;
            // For all but the last node at the current level, we want "│ " in indentation; for last, "  "
            String newIndent = indent + (last ? "  " : "│ ");
            printComponentTreeRec(entry.getKey(), entry.getValue(), newIndent, idx == sz);
        }
    }

    /**
     * Add a hierarchy node component of given path that doesn't do anything itself.
     */
    @ApiStatus.NonExtendable
    public default void addNode(HierarchyPath path) {
        this.addSubComponentByPath(path, EntityComponentTypes.NODE.get().create(this.getEntity()));
    }

    /**
     * Add a hierarchy node component of given path that doesn't do anything itself.
     */
    @ApiStatus.NonExtendable
    public default void addNode(String path) {
        this.addNode(HierarchyPath.byLiteral(path));
    }
}