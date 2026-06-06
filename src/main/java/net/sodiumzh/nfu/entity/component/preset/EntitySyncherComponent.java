package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.sodiumzh.nfu.annotation.DontCallManually;
import net.sodiumzh.nfu.entity.component.EntityComponentBase;
import net.sodiumzh.nfu.exception.WrongSideException;
import net.sodiumzh.nfu.network.NFUDataSerializer;
import net.sodiumzh.nfu.network.NFUNetworkChannels;
import net.sodiumzh.nfu.object.ServerOnly;
import net.sodiumzh.nfu.util.NFUDebugStatics;
import net.sodiumzh.nfu.util.NFUNetworkStatics;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EntitySyncherComponent<E extends Entity> extends EntityComponentBase<E> {

    protected Map<String, SynchedData> synchedData = new HashMap<>();
    protected Map<String, SynchedGetter> synchedGetters = new HashMap<>();
    private Set<String> changedDataKeys = new HashSet<>();
    long lastReceivedPacketId = -1; // accessed on client, to prevent out-of-date packets received after a newer packet
    private int syncInterval = 1;

    public EntitySyncherComponent(E entity) {
        super(entity);
    }

    /**
     * Create a synched data. Synched data must be created before other any operations.
     * @param <T>Data class. Exactly same to the data class in {@code dataSerialzier}.
     * @param key Data key as string.
     * @param dataSerialzier Data serializer applied.
     * @param initValue Default value if not set.
     */
    public <T> void createSynchedData(String key, NFUDataSerializer<T> dataSerialzier, T initValue, boolean shouldSave) {
        synchedData.put(key, new SynchedData(this, dataSerialzier.getObjectClass(), dataSerialzier, initValue, shouldSave));
    }

    public <T> boolean hasSynchedData(String key, Class<T> dataType) {
        return synchedData.containsKey(key) && synchedData.get(key).type.isAssignableFrom(dataType);
    }

    public <T> Optional<T> getSynchedData(String key, Class<T> dataClass) {
        if (hasSynchedData(key, dataClass)) {
            // Access also label dirty because an object may be changed internally without changing the reference
            this.changedDataKeys.add(key);
            return Optional.ofNullable((T)synchedData.get(key).get());
        }
        else return Optional.empty();
    }

    public <T> Optional<T> getSynchedDataUnchecked(String key) {
        if (hasSynchedData(key, Object.class)) {
            // Access also label dirty because an object may be changed internally without changing the reference
            this.changedDataKeys.add(key);
            return Optional.ofNullable((T)synchedData.get(key).get());
        }
        else return Optional.empty();
    }

    /**
     * Set synched data value. Only on server. Invocation on client will do nothing.
     * @param key Synched data key
     * @param dataClass Expected data class. <i>This is a salt to ensure you know what class this field expects to receive.</i>
     * @param value New value.
     */
    public <T> void setSynchedData(String key, Class<T> dataClass, T value) {
        if (this.getEntity().level().isClientSide)
            return;
        if (this.hasSynchedData(key, dataClass)) {
            this.synchedData.get(key).value = value;
            this.changedDataKeys.add(key);
        }
        else NFUDebugStatics.errorOnce("Access of an undefined or class-mismatching synched data '" + key + "'");
    }

    /**
     * Invoked only on sync.
     */
    @ApiStatus.Internal
    public void setSynchedDataClient(String key, Object value) {
        if (this.getEntity().level().isClientSide)
            throw new WrongSideException("setSynchedDataClient invoked on server.");
        if (this.hasSynchedData(key, Object.class)) {
            this.synchedData.get(key).value = value;
        } else NFUDebugStatics.errorOnce("Access of an undefined or class-mismatching synched data '" + key + "'");
    }

    /**
     * Define a synched getter. Synched getters get from a {@link Supplier} every tick from server and store it on client.
     * They are not saved into data.
     * <p>When a synched getter is accessed on the client, it will read the cache value synched from server (if no synching happened,
     * it's the default value).
     */
    public <T> void createSynchedGetter(String key, NFUDataSerializer<T> serializer, @Nullable T defaultValue, Function<E, T> accessorOnServer) {
        synchedGetters.put(key, new SynchedGetter(this, serializer.getObjectClass(), serializer, accessorOnServer, defaultValue));
    }

    public boolean hasSynchedGetter(String key, Class<?> type) {
        return this.synchedGetters.containsKey(key) && this.synchedGetters.get(key).type.isAssignableFrom(type);
    }

    /**
     * Get synched getter from key and type.
     * <p>Safe to call on both sides. On server, it will be directly accessed by the supplier,
     * and on client it will be read from the cached field which is updated on synching.
     * <p>Note: this includes an unsafe casting. Double-check the type before using.
     * Return empty if not present.
     */
    public <T> Optional<T> getSynchedGetter(String key, Class<T> type) {
        if (!this.hasSynchedGetter(key, type)) {
            NFUDebugStatics.errorOnce(EntitySyncherComponent.class, this.getPathFromRoot() + " accessing missing getter \"" + key + "\"");
            return Optional.empty();
        }
        if (this.getEntity().level().isClientSide)
            return Optional.ofNullable((T)this.synchedGetters.get(key).cache);
        else
            return Optional.ofNullable((T)this.synchedGetters.get(key).getter.apply(this.getEntity()));
    }

    /**
     * Get a synched getter as a raw {@link Object}.
     * <p>Safe to call on both sides. On server, it will be directly accessed by the supplier,
     * and on client it will be read from the cached field which is updated on synching.
     * <p> Null if the key doesn't exist.
     */
    @Nullable
    public Optional<Object> getSynchedGetter(String key) {
        return getSynchedGetter(key, Object.class);
    }

    /**
     * Set the key-value pair in the synched getter cache on client.
     * <p>This is only used in synching process and should not be called elsewhere.
     * Safe to call on server as the cache on server will not be read.
     */
    @DontCallManually
    public void setSynchedGetterClient(String key, @Nullable Object o) {
        if (!this.getEntity().level().isClientSide)
            throw new WrongSideException("setSynchedGetterClient invoked on server.");
        if (!this.hasSynchedGetter(key, Object.class))
            NFUDebugStatics.errorOnce("Access of an undefined or class-mismatching synched getter '" + key + "'");
        else {
            this.synchedGetters.get(key).setCachedValueUnchecked(o);
        }
    }

    public int getSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(int syncInterval) {
        this.syncInterval = syncInterval;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        this.synchedData.forEach((key, value) -> nbt.put(key, ((NFUDataSerializer<Object>) value.serializer).toTag(value.value)));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.synchedData.forEach((key, value) -> {
            if (nbt.contains(key)) {
                try {
                    value.value = value.serializer.fromTag(nbt.get(key));
                } catch (Exception e) {
                    NFUDebugStatics.errorOnce(EntitySyncherComponent.class,
                        "NFU: Failed to load synched data \"" + key + "\" for entity component "
                            + this.getEntity().getName().getString() + this.getPathFromRoot() + "\"");
                }
            }
        });
    }

    public void tick() {
        if (!this.isClientSide()) {
            this.synchedGetters.forEach((k, g) -> {
                g.cache = g.getter.apply(this.getEntity());
            });
            // Sync
            if ((this.getEntity().tickCount % this.syncInterval) == 0) {
                ClientboundEntitySyncherComponentSyncPacket packet = new ClientboundEntitySyncherComponentSyncPacket(this);
                NFUNetworkStatics.sendToAllPlayers(this.getEntity().level(), NFUNetworkChannels.CHANNEL, packet);
                this.changedDataKeys.clear();
            }
        }
    }

    /**
     * As ticking here only handles synching, stop ticking when unchanged and prevent packet synching to save resource
     */
    public boolean shouldTick() {
        return !this.changedDataKeys.isEmpty() || !this.synchedGetters.isEmpty();
    }

    /**
     * Manually sync all data and getters from server to client. Do nothing if invoked on client.
     */
    public void syncAll() {
        if (this.isClientSide()) return;
        if (this.synchedData.isEmpty() && this.synchedGetters.isEmpty()) return;
        // Update synched getters
        this.synchedGetters.forEach((k, g) -> {
            g.cache = g.getter.apply(this.getEntity());
        });
        // Sync
        // Label all keys changed, so that we sync all data
        this.changedDataKeys.addAll(this.synchedData.keySet());
        ClientboundEntitySyncherComponentSyncPacket packet = new ClientboundEntitySyncherComponentSyncPacket(this);
        NFUNetworkStatics.sendToAllPlayers(this.getEntity().level(), NFUNetworkChannels.CHANNEL, packet);
        this.changedDataKeys.clear();
    }

    public static class SynchedData {
        private final EntitySyncherComponent<? extends Entity> owner;
        public final Class<?> type;
        public final NFUDataSerializer<?> serializer;
        // Cached value. On client this value is synched from server and taken on getting value. On server this value
        // is cached to track value change.
        @Nullable public Object value;
        public final boolean save;

        protected <T> SynchedData(EntitySyncherComponent<? extends Entity> owner,
                              Class<T> type, NFUDataSerializer<? extends T> serializer, @Nullable T defaultValue, boolean shouldSave) {
            this.owner = owner;
            this.type = type;
            this.serializer = serializer;
            this.value = defaultValue;
            this.save = shouldSave;
        }
        public @Nullable Object get() {
            return value;
        }
    }

    public static class SynchedGetter {
        private final EntitySyncherComponent<? extends Entity> owner;
        public final Class<?> type;
        public final Function<Entity, ?> getter;
        public final NFUDataSerializer<?> serializer;
        public @Nullable Object cache;  // Accessed on client. Tracked on server tick to find if it's changed

        protected <E extends Entity, T> SynchedGetter(EntitySyncherComponent<E> owner, Class<T> type, NFUDataSerializer<? extends T> serializer,
                                Function<E, ? extends T> getter, @Nullable T defaultValue) {
            this.owner = owner;
            this.type = type;
            this.getter = (e -> getter.apply((E)e));
            this.serializer = serializer;
            this.cache = defaultValue;
        }

        public @Nullable Object get() {
            if (owner.getEntity().level().isClientSide)
                return this.cache;
            else return getter.apply(owner.getEntity());
        }

        public void setCachedValueUnchecked(Object value) {
            this.cache = value;
        }

    }

    public static class ClientboundEntitySyncherComponentSyncPacket implements Packet<ClientGamePacketListener> {

        private static final ServerOnly<Long> CURRENT_PACKET_ID = new ServerOnly<>(0L);
        public final long packetId;
        @Nullable   // Non-null on server, null on client
        public final EntitySyncherComponent<? extends Entity> syncher;
        public final int entityID;
        public final String componentPath;
        public final Map<String, ValueEntry> data = new HashMap<>();
        public final Map<String, ValueEntry> getters = new HashMap<>();

        public ClientboundEntitySyncherComponentSyncPacket(EntitySyncherComponent<? extends Entity> syncher) {
            this.packetId = CURRENT_PACKET_ID.get();
            CURRENT_PACKET_ID.set(CURRENT_PACKET_ID.get() + 1);
            this.syncher = syncher;
            this.entityID = syncher.getEntity().getId();
            this.componentPath = syncher.getPathFromRoot();
            for (String key: syncher.changedDataKeys) {
                this.data.put(key, new ValueEntry(syncher.synchedData.get(key).serializer, syncher.synchedData.get(key).value));
            }
            for (String key: syncher.synchedGetters.keySet()) {
                this.getters.put(key, new ValueEntry(syncher.synchedGetters.get(key).serializer, syncher.synchedGetters.get(key).cache));
            }
        }

        public ClientboundEntitySyncherComponentSyncPacket(FriendlyByteBuf buf) {
            this.packetId = buf.readLong();
            this.syncher = null;
            this.entityID = buf.readInt();
            this.componentPath = buf.readUtf();
            this.data.putAll(buf.readMap(FriendlyByteBuf::readUtf, ValueEntry::read));
            this.getters.putAll(buf.readMap(FriendlyByteBuf::readUtf, ValueEntry::read));
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeLong(this.packetId);
            buf.writeInt(this.entityID);
            buf.writeUtf(componentPath);
            buf.writeMap(data, FriendlyByteBuf::writeUtf, (b, v) -> v.write(b));
            buf.writeMap(getters, FriendlyByteBuf::writeUtf, (b, v) -> v.write(b));
        }

        public Map<String, Optional<Object>> dataValues() {
            return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Optional.ofNullable(e.getValue().value())));
        }

        public Map<String, Optional<Object>> getterValues() {
            return getters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Optional.ofNullable(e.getValue().value())));
        }

        @Override
        public void handle(ClientGamePacketListener pHandler) {
            EntityComponentPresetClientPacketHandlers.HandleEntitySyncherComponentSync(this, pHandler);
        }

        public static record ValueEntry(@Nullable NFUDataSerializer<?> serializer, @Nullable Object value) {

            public void write(FriendlyByteBuf buf) {
                buf.writeBoolean(value != null);
                if (value != null) {
                    buf.writeResourceLocation(serializer.getKey());
                    ((NFUDataSerializer<Object>)serializer).write(buf, value);
                }
            }

            public static ValueEntry read(FriendlyByteBuf buf) {
                if (buf.readBoolean()) {
                    NFUDataSerializer<?> s = NFUDataSerializer.fromId(buf.readResourceLocation());
                    Object v = s.read(buf);
                    return new ValueEntry(s, v);
                }
                else return new ValueEntry(null, null);
            }

        };
    }

}
