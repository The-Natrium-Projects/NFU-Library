package net.sodiumzh.nfu.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.INBTSerializable;
import net.sodiumzh.nfu.registry.NFUCaps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A simple capability serving as an additional data container.
 */
public interface CEntityDataCapability extends INBTSerializable<CompoundTag> {

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

    public static class Impl implements CEntityDataCapability {

        private CompoundTag nbt = new CompoundTag();
        private final Map<String, Object> transientParameters = new HashMap<>();

        public Impl(){}

        @Override
        public CompoundTag getNBT() {
            return nbt;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> getTransientParameter(String key, Class<T> asClass) {
            if (transientParameters.containsKey(key)
                && asClass.isAssignableFrom(transientParameters.get(key).getClass()))
            {
                return Optional.of((T) (transientParameters.get(key)));
            }
            else return Optional.empty();
        }

        @Override
        public Optional<Object> getTransientParameter(String key) {
            return Optional.ofNullable(transientParameters.get(key));
        }

        @Override
        public void putTransientParameter(String key, @Nullable Object parameter) {
            if (parameter == null)
                transientParameters.remove(key);
            else transientParameters.put(key, parameter);
        }

        @Override
        public void removeTransientParameter(String key) {
            transientParameters.remove(key);
        }

        @Override
        public CompoundTag serializeNBT() {
            return nbt.copy();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            this.nbt = nbt.copy();
        }
    }

    public static CEntityDataCapability get(Entity e) {
        return e.getCapability(NFUCaps.CAP_ENTITY_DATA).orElse(new Impl());
    }

}
