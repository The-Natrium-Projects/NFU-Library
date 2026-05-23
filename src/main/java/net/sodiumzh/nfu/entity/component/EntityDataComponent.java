package net.sodiumzh.nfu.entity.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.sodiumzh.nfu.network.NFUDataSerializer;
import net.sodiumzh.nfu.util.NFUNBTStatics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EntityDataComponent<E extends Entity> extends EntityComponentBase<E> {

    public EntityDataComponent(E entity) {
        super(entity);
    }

    private final Map<String, Entry> variableTable = new HashMap<>();
    private CompoundTag nbt = new CompoundTag();

    /**
     * Check if a variable of given key is defined. A key is defined when {@code putTransientVariable} or {@code putPermanentVariable} is called.
     * <p>Note: True result doesn't guarantee non-null. A defined variable is still nullable.
     */
    public boolean isPresent(String key) {
        return variableTable.containsKey(key);
    }

    /**
     * Check if a variable of given key is defined and transient (not saved/loaded).
     * A key is defined when {@code putTransientVariable} or {@code putPermanentVariable} is called.
     * <p>Note: True result doesn't guarantee non-null. A defined variable is still nullable.
     */
    public boolean isTransient(String key) {
        return variableTable.containsKey(key) && !variableTable.get(key).isPermanent();
    }

    /**
     * Check if a variable of given key is defined and permanent (saved/loaded).
     * A key is defined when {@code putTransientVariable} or {@code putPermanentVariable} is called.
     * <p>Note: True result doesn't guarantee non-null. A defined variable is still nullable.
     */
    public boolean isPermanent(String key) {
        return variableTable.containsKey(key) && variableTable.get(key).isPermanent();
    }

    /**
     * Get transient variable by key and class. Returns empty if the value is absent or the value class doesn't match
     * the given class.
     * <p>Note: Transient variables are thread-separated (side-separated) and are not serialized.
     * Use NBT (available only on server) to write serialized data.</p>
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getVariable(String key, Class<T> asClass) {
        if (variableTable.containsKey(key) && variableTable.get(key) != null
            && asClass.isAssignableFrom(variableTable.get(key).getClass()))
        {
            return Optional.of((T) (variableTable.get(key)));
        }
        else return Optional.empty();
    }

    /**
     * Get variable by key and class. Returns empty if the value is absent.
     * <p>Note: Transient variables are thread-separated (side-separated) and are not serialized or synched.
     * Use NBT (available only on server) to write serialized data.</p>
     */
    public Optional<Object> getVariable(String key) {
        return Optional.ofNullable(variableTable.get(key));
    }

    /**
     * Get additional NBT. NBT data will be all saved & loaded.
     */
    public CompoundTag getNBT() {
        return this.nbt;
    }

    /**
     * Add or set a transient variable.
     * <p>Note: Transient variables are thread-separated (side-separated) and are not serialized.
     * Use NBT (available only on server) to write serialized data.</p>
     */
    public void putTransientVariable(String key, @Nullable Object value) {
        if (key.equals("nbt")) {
            throw new IllegalArgumentException("Illegal key \"nbt\": reserved for NBT serialization.");
        }
        if (variableTable.get(key) != null && variableTable.get(key).isPermanent()) {
            throw new IllegalStateException("Entity data component \"" + this.getPathFromRoot() + "\" illegal variable operation: "
                + "attempting to put a transient variable to key \"" + key + "\", but the key is used as permanent.");
        }
        this.variableTable.put(key, new Entry(value, null));
    }

    public void putPermanentVariable(String key, @Nullable Object value, @Nonnull NFUDataSerializer<?> serializer) {
        if (key.equals("nbt")) {
            throw new IllegalArgumentException("Illegal key \"nbt\": reserved for NBT serialization.");
        }
        if (variableTable.get(key) != null) {
            if (!variableTable.get(key).isPermanent()) {
                throw new IllegalStateException("Entity data component \"" + this.getPathFromRoot() + "\" illegal variable operation: "
                    + "attempting to put a permanent variable to key \"" + key + "\", but the key is used as transient.");
            }
            else if (!variableTable.get(key).serializer().getKey().equals(serializer.getKey())) {
                throw new IllegalStateException("Entity data component \"" + this.getPathFromRoot() + "\" illegal variable operation: "
                    + "attempting to put a permanent variable to key \"" + key + "\" using serializer " + serializer.getKey().toString()
                    + ", but this key is using serializer " + variableTable.get(key).serializer.getKey().toString() + ".");
            }
        }
        if (value != null && !serializer.getObjectClass().isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("Entity data component \"" + this.getPathFromRoot() + "\" illegal variable operation: "
                + "attempting to put a permanent variable to key \"" + key + "\" using serializer " + serializer.getKey().toString()
                + "for type " + serializer.getObjectClass().getSimpleName() +
                ", but the object is " + value.getClass().getSimpleName() + ".");
        }
        this.variableTable.put(key, new Entry(value, serializer));
    }

    public void putPermanentVariable(String key, @Nullable Object value) {
        if (key.equals("nbt")) {
            throw new IllegalArgumentException("Illegal key \"nbt\": reserved for NBT serialization.");
        }
        if (!variableTable.containsKey(key)) {
            throw new IllegalStateException("Entity data component \"" + this.getPathFromRoot() + "\" illegal variable operation: "
                + "attempting to put a permanent variable to key \"" + key + "\" without specifying the serializer, but the serializer is absent. Call serializer-specific version at least once.");
        }
        this.putPermanentVariable(key, value, variableTable.get(key).serializer());
    }

    /**
     * Get a variable of given type, or compute from supplier and put transient if the variable is absent.
     * <p>Note: If the variable is present and permanent, it will get the value and will not convert the variable to transient.
     * <p>Note: The return value of the supplier can be nullable.
     */
    public <T> Optional<T> getOrPutTransient(String key, Class<T> type, Supplier<? extends T> supplier) {
        if (key.equals("nbt")) {
            throw new IllegalArgumentException("Illegal key \"nbt\": reserved for NBT serialization.");
        }
        if (!this.isPresent(key)) {
            @Nullable T v = supplier.get();
            this.putTransientVariable(key, v);
            return Optional.ofNullable(v);
        }
        else return this.getVariable(key, type);
    }

    /**
     * Get a variable of given type, or compute from supplier and put permanent if the variable is absent.
     * <p>Note: If the variable is present and transient, it will get the value and will not convert the variable to permanent.
     * <p>Note: The return value of the supplier can be nullable.
     */
    public <T> Optional<T> getOrPutPermanent(String key, Class<T> type, @Nonnull NFUDataSerializer<?> serializer, Supplier<? extends T> supplier) {
        if (key.equals("nbt")) {
            throw new IllegalArgumentException("Illegal key \"nbt\": reserved for NBT serialization.");
        }
        if (!this.isPresent(key)) {
            @Nullable T v = supplier.get();
            this.putPermanentVariable(key, v, serializer);
            return Optional.ofNullable(v);
        }
        else return this.getVariable(key, type);
    }

    /**
     * Get a copy of all defined keys.
     */
    public Set<String> keySet() {
        return Set.copyOf(this.variableTable.keySet());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("nbt", this.nbt.copy());
        this.variableTable.entrySet().stream().filter(entry -> entry.getValue().isPermanent())
            .forEach(entry -> {
                CompoundTag entryNBT = new CompoundTag();
                entryNBT.putBoolean("isPresent", entry.getValue().value() != null);
                entryNBT.putString("serializer", entry.getValue().serializer().getKey().toString());
                if (entry.getValue().value() != null)
                    entryNBT.put("value", entry.getValue().serializer().toTag(entry.getValue().value()));
            });
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.nbt = nbt.getCompound("nbt").copy();
        NFUNBTStatics.forEach(nbt, (k, v) -> {
            if (v instanceof CompoundTag entryNBT) {
                boolean isPresent = entryNBT.getBoolean("isPresent");
                NFUDataSerializer<?> serializer = NFUDataSerializer.getFromRegistry(new ResourceLocation(entryNBT.getString("serializer")));
                if (serializer == null) return;
                if (!isPresent) {
                    this.putPermanentVariable(k, null, serializer);
                }
                else {
                    Object value = serializer.fromTag(entryNBT.get("value"));
                    this.putPermanentVariable(k, value, serializer);
                }
            }
        });
    }

    @Override
    public void tick() {
    }

    protected static record Entry(@Nullable Object value, @Nullable NFUDataSerializer serializer)
    {

        public boolean isPermanent() {
            return serializer != null;
        }

        public Optional<Tag> save() {
            if (this.value == null) return Optional.empty();
            try {
                return Optional.ofNullable(serializer.toTag(this.value));
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        }

        public static Entry load(Tag nbt, @Nonnull NFUDataSerializer serializer) {
            try {
                return new Entry(serializer.fromTag(nbt), serializer);
            } catch (RuntimeException e) {
                return new Entry(null, serializer);
            }
        }
    }

}
