package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.event.NFUEntityEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityComponentSetupEvent extends NFUEntityEvent<Entity> {

    private final CEntityComponentManager manager;
    private final Map<String, EntityComponentType<? extends Entity, ?>> componentTypes = new HashMap<>();

    public EntityComponentSetupEvent(Entity entity, CEntityComponentManager manager) {
        super(entity);
        this.manager = manager;
    }

    public void addComponent(String path, EntityComponentType<? extends Entity, ?> type) {
        String newPath = Arrays.stream(path.split("[/\\\\]+")).filter(str -> !str.isEmpty())
            .map(str -> "/" + str).reduce("", (s1, s2) -> s1 + s2);
        // Duplication handling
        if (componentTypes.containsKey(newPath)) {
            // Node doesn't overwrite anything
            if (type.equals(EntityComponentTypes.NODE.get())) {}
            // Other components overwrite node
            else if (componentTypes.get(newPath).equals(EntityComponentTypes.NODE.get()))
                componentTypes.put(newPath, type);
            // Occupied with same type, no conflict
            else if (type.equals(componentTypes.get(newPath))) {}
            // Of different types, and both not node, conflict
            else throw new IllegalArgumentException("Duplicate key " + newPath);
        }
        else componentTypes.put(newPath, type);
    }

    public void addNode(String path) {
        addComponent(path, EntityComponentTypes.NODE.get());
    }

    /**
     * Add a component at given path, and fill its upstream paths with nodes if absent.
     * <p>Example: {@code addComponentAndNodes("/a/b/c", type)} will add a component of given type
     * at {@code "/a/b/c"}, and add nodes at {@code "/a"} and {@code "/a/b"}. If an upstream node is already
     * occupied, it will be ignored.
     */
    public void addComponentAndUpstreamNodes(String path, EntityComponentType<? extends Entity, ?> type) {
        StringBuilder currentPath = new StringBuilder();
        List<String> split = Arrays.stream(path.split("[/\\\\]+")).filter(str -> !str.isEmpty())
            .map(str -> "/" + str).toList();
        for (int i = 0; i < split.size(); ++i) {
            currentPath.append(split.get(i));
            if (i == split.size() - 1)
                this.addComponent(currentPath.toString(), type);
            else this.addNode(currentPath.toString());
        }
    }

    @ApiStatus.Internal
    public void collect() {
        componentTypes.entrySet().stream().sorted((e1, e2) -> {
            if (e1.getKey().equals(e2.getKey())) return 0;
            else if (e1.getKey().startsWith(e2.getKey())) return -1;
            else if (e2.getKey().startsWith(e1.getKey())) return 1;
            else return 0;
        }).forEach(e -> this.manager.addSubComponentByPath(e.getKey(), e.getValue().createUnsafe(this.getEntity())));
    }
}
