package net.sodiumzh.nfu.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.INBTSerializable;
import net.sodiumzh.nfu.entity.component.CEntityComponentManager;
import net.sodiumzh.nfu.entity.component.EntityComponentTypes;
import net.sodiumzh.nfu.entity.component.EntityDynamicDataComponent;
import net.sodiumzh.nfu.registry.NFUCapabilities;
import org.checkerframework.checker.units.qual.C;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A simple capability serving as an additional data container.
 */
@Deprecated(since = "0.x.32", forRemoval = true)
public interface CEntityDataCapability extends INBTSerializable<CompoundTag> {

    public Entity getEntity();

    /**
     * Get root NBT which will be serialized into/deserialized from the entity save data.
     */
    public CompoundTag getNBT();

    /**
     * Get an {@link Optional} of transient parameter with given key if it exists and matches
     * the given class. If absent or class not matching, return empty.
     * <p>Note: Transient parameters are not serialized and will be cleared upon restarting.
     */
    public <T> Optional<T> getTransientParameter(String key, Class<T> asClass);

    /**
     * Get an {@link Optional} of transient parameter as raw {@link Object}. Empty if absent.
     * <p>Note: Transient parameters are not serialized and will be cleared upon restarting.
     */
    public Optional<Object> getTransientParameter(String key);

    /**
     * Put a transient parameter of given key. Null input = removing the parameter.
     * <p>Note: Transient parameters are not serialized and will be cleared upon restarting.
     */
    public void putTransientParameter(String key, Object parameter);

    /**
     * Remove the transient parameter of the given key.
     * <p>Note: Transient parameters are not serialized and will be cleared upon restarting.
     */
    public void removeTransientParameter(String key);

    @Deprecated
    public static class Impl implements CEntityDataCapability {

        private CompoundTag nbt = new CompoundTag();
        private final Map<String, Object> transientParameters = new HashMap<>();
        private final Entity entity;

        public Impl(Entity e){this.entity = e;}

        @Override
        public Entity getEntity() {
            return entity;
        }

        private Optional<EntityDynamicDataComponent> getDataComponent() {
            return CEntityComponentManager.getManager(this.getEntity())
                .getSubComponent("dynamic_data", EntityComponentTypes.DYNAMIC_DATA.get());
        }

        @Override
        public CompoundTag getNBT() {
            return getDataComponent()
                .map(EntityDynamicDataComponent::getNBT).orElse(new CompoundTag());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> getTransientParameter(String key, Class<T> asClass) {
            return getDataComponent().flatMap(c -> c.getVariable(key, asClass));
        }

        @Override
        public Optional<Object> getTransientParameter(String key) {
            return getDataComponent().map(c -> c.getVariable(key));
        }

        @Override
        public void putTransientParameter(String key, @Nullable Object parameter) {
            getDataComponent().ifPresent(c -> c.putVariable(key, parameter));
        }

        @Override
        public void removeTransientParameter(String key) {

            getDataComponent().ifPresent(c -> c.removeVariable(key));
        }

        @Override
        public CompoundTag serializeNBT() {
            var c = getDataComponent().orElse(null);
            if (c != null) {
                this.nbt.getAllKeys().forEach(key -> {
                    if (!c.getNBT().contains(key))
                        c.getNBT().put(key, this.nbt.get(key));
                });
            }
            // Keep the porting process in the next 3 updates
            return this.nbt.copy();
        }

        @Override
        public void deserializeNBT(CompoundTag inNBT) {
            var c = getDataComponent().orElse(null);
            if (c != null) {
                inNBT.getAllKeys().forEach(key -> {
                    if (!c.getNBT().contains(key))
                        c.getNBT().put(key, inNBT.get(key));
                });
            }
            // Keep the porting process in the next 3 updates
            this.nbt = inNBT.copy();
        }
    }

    @Deprecated
    public static CEntityDataCapability get(Entity e) {
        return e.getCapability(NFUCapabilities.CAP_ENTITY_DATA).orElse(new Impl(e));
    }

}
