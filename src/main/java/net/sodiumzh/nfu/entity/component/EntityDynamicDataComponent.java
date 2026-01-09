package net.sodiumzh.nfu.entity.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EntityDynamicDataComponent extends EntityComponentBase {

    public EntityDynamicDataComponent(Entity entity) {
        super(entity);
    }

    private CompoundTag nbt = new CompoundTag();
    private final Map<String, Object> variableTable = new HashMap<>();

    public CompoundTag getNBT() {
        return nbt;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getVariable(String key, Class<T> asClass) {
        if (variableTable.containsKey(key)
            && asClass.isAssignableFrom(variableTable.get(key).getClass()))
        {
            return Optional.of((T) (variableTable.get(key)));
        }
        else return Optional.empty();
    }

    public Optional<Object> getVariable(String key) {
        return Optional.ofNullable(variableTable.get(key));
    }

    public void putVariable(String key, @Nullable Object parameter) {
        if (parameter == null)
            variableTable.remove(key);
        else variableTable.put(key, parameter);
    }

    public void removeVariable(String key) {
        variableTable.remove(key);
    }

    public Map<String, Object> getVariablesCopy() {
        return Map.copyOf(variableTable);
    }

    @Override
    public CompoundTag serializeNBT() {
        return nbt.copy();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.nbt = nbt.copy();
    }

    @Override
    public void tick() {
    }

    @Override
    public EntityComponentType<?> getType() {
        return EntityComponentTypes.DATA.get();
    }
}
