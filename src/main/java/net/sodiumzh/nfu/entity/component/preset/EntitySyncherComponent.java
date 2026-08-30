package net.sodiumzh.nfu.entity.component.preset;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.sodiumzh.nfu.annotation.DontCallManually;
import net.sodiumzh.nfu.entity.component.EntityComponentAPI;
import net.sodiumzh.nfu.entity.component.EntityComponentBase;
import net.sodiumzh.nfu.exception.WrongSideException;
import net.sodiumzh.nfu.level.HitResultInfo;
import net.sodiumzh.nfu.network.NFUDataSerializer;
import net.sodiumzh.nfu.network.NFUDataSerializers;
import net.sodiumzh.nfu.network.NFUNetworkChannels;
import net.sodiumzh.nfu.object.ClientOnly;
import net.sodiumzh.nfu.object.HierarchyPath;
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
    Set<String> changedDataKeys = new HashSet<>();
    // Accessed on client, to prevent out-of-date packets received after a newer packet.
    // For auto sync
    long lastReceivedPacketId = -1;
    // For manual sync
    long lastReceivedManualPacketId = -1;
    private int syncInterval = 1;

    public EntitySyncherComponent(E entity) {
        super(entity);
    }

    /**
     * Create a synched data. Synched data must be created before other any operations.
     * @param <T>Data class. Exactly same to the data class in {@code dataSerializer}.
     * @param key Data key as string.
     * @param dataSerializer Data serializer applied.
     * @param initValue Default value if not set.
     */
    public <T> void createSynchedData(String key, NFUDataSerializer<T> dataSerializer, T initValue, boolean shouldSave) {
        synchedData.put(key, new SynchedData(this, dataSerializer.getObjectClass(), dataSerializer, initValue, shouldSave));
    }

    public <T> boolean hasSynchedData(String key, Class<T> dataType) {
        return synchedData.containsKey(key) && dataType.isAssignableFrom(synchedData.get(key).type);
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
        if (this.getEntity().getLevel().isClientSide)
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
        if (!this.getEntity().getLevel().isClientSide)
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
     * <p>Note: client-to-server synched getter is <b>ONLY FOR PLAYER</b>. Using on other entities will cause an exception.
     * @param key String key identifier.
     * @param serializer Serializer for synching.
     * @param defaultValue Default fallback value before receiving synching.
     * @param direction Synching direction. Mostly it's server-to-client. Client-to-server synching is ONLY FOR PLAYERS.
     * @param accessorOnMainSide The accessor method on the main side. It will be invoked on the main side's each synching operation and update
     *                           the other side's cached value.
     */
    public <T> void createSynchedGetter(String key, NFUDataSerializer<T> serializer, @Nullable T defaultValue, SynchedGetter.Direction direction,
                                        Function<E, T> accessorOnMainSide) {
        if (direction.equals(SynchedGetter.Direction.CLIENT_TO_SERVER) && !(this.getEntity() instanceof Player)) {
            throw new UnsupportedOperationException("Client-to-server synched getter can only used on players.");
        }
        synchedGetters.put(key, new SynchedGetter(this, serializer.getObjectClass(), serializer, accessorOnMainSide, defaultValue, direction));
    }

    /**
     * Define a server-to-client synched getter. Synched getters get from a {@link Supplier} every tick from server and store it on client.
     * They are not saved into data.
     * <p>When a synched getter is accessed on the client, it will read the cache value synched from server (if no synching happened,
     * it's the default value).
     * <p>Note: client-to-server synched getter is <b>ONLY FOR PLAYER</b>. Using on other entities will cause an exception.
     * @param key String key identifier.
     * @param serializer Serializer for synching.
     * @param defaultValue Default fallback value before receiving synching.
     * @param accessorOnServer The accessor method on the main side. It will be invoked on the main side's each synching operation and update
     *                           the other side's cached value.
     */
    public <T> void createSynchedGetter(String key, NFUDataSerializer<T> serializer, @Nullable T defaultValue,
                                        Function<E, T> accessorOnServer) {
        synchedGetters.put(key, new SynchedGetter(this, serializer.getObjectClass(), serializer, accessorOnServer, defaultValue, SynchedGetter.Direction.SERVER_TO_CLIENT));
    }

    public boolean hasSynchedGetter(String key, Class<?> type) {
        return this.synchedGetters.containsKey(key) && type.isAssignableFrom(this.synchedGetters.get(key).type);
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
        if (this.synchedGetters.get(key).direction.isMainSide(this.isClientSide()))
            return Optional.ofNullable((T)this.synchedGetters.get(key).getter.apply(this.getEntity()));
        else
            return Optional.ofNullable((T)this.synchedGetters.get(key).cache);

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
    @ApiStatus.Internal
    public void setSynchedGetterCachedValueOnSynchedSide(String key, @Nullable Object o) {
        if (!this.hasSynchedGetter(key, Object.class))
            NFUDebugStatics.errorOnce("Access of an undefined or class-mismatching synched getter '" + key + "'");
        else if (this.synchedGetters.get(key).direction.isMainSide(this.isClientSide()))
            throw new WrongSideException("This operation is synched-side-only, but invoked on the main side.");
        else
            this.synchedGetters.get(key).setCachedValueUnchecked(o);
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
        if (this.isClientSide() && this.getEntity() instanceof Player player) {
            // Handle player client2server sync
            if ((this.getEntity().tickCount % this.syncInterval) == 0) {
                ServerboundPlayerEntitySyncherComponentSyncPacket packet = new ServerboundPlayerEntitySyncherComponentSyncPacket(this);
                NFUNetworkStatics.sendToServer(player, NFUNetworkChannels.CHANNEL, packet);
                this.changedDataKeys.clear();
            }
            // Sync once on the first tick
            if (this.getEntity().tickCount == 1)
                this.sync();
        }
    }

    /**
     * Sync all entities of a level.
     */
    public static void syncAll(ServerLevel level, boolean ignoresSyncInterval) {
        ClientboundEntitySyncherComponentSyncAllPacket packet =
            new ClientboundEntitySyncherComponentSyncAllPacket(level, ignoresSyncInterval);
        NFUNetworkStatics.sendToAllPlayers(level, NFUNetworkChannels.CHANNEL, packet);
        packet.handledComponentPaths.forEach((uuid, path) -> {
            Optional.ofNullable(uuid).map(level::getEntity)
                .flatMap(e -> EntityComponentAPI.getComponentByPath(e, path))
                .filter(c -> c instanceof EntitySyncherComponent<? extends Entity>)
                .map(c -> (EntitySyncherComponent<?>)c)
                .ifPresent(c -> c.changedDataKeys.clear());
        });
    }

    public boolean shouldSync() {
        return !this.synchedData.isEmpty() || !this.synchedGetters.isEmpty();
    }

    /**
     * Manually sync all data and getters from server to client. Do nothing if invoked on client.
     */
    public void sync() {
        if (this.isClientSide()) return;
        if (!shouldSync()) return;
        // Update synched getters
        this.synchedGetters.forEach((k, g) -> {
            g.cache = g.getter.apply(this.getEntity());
        });
        // Sync
        // Label all keys changed, so that we sync all data
        this.changedDataKeys.addAll(this.synchedData.keySet());
        ClientboundEntitySyncherComponentSyncPacket packet = new ClientboundEntitySyncherComponentSyncPacket(this);
        NFUNetworkStatics.sendToAllPlayers(this.getEntity().getLevel(), NFUNetworkChannels.CHANNEL, packet);
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
        public final Direction direction;

        protected <E extends Entity, T> SynchedGetter(EntitySyncherComponent<E> owner, Class<T> type, NFUDataSerializer<? extends T> serializer,
                                Function<E, ? extends T> getter, @Nullable T defaultValue, Direction direction) {
            this.owner = owner;
            this.type = type;
            this.getter = (e -> getter.apply((E)e));
            this.serializer = serializer;
            this.cache = defaultValue;
            this.direction = direction;
        }

        public @Nullable Object get() {
            if (owner.getEntity().getLevel().isClientSide)
                return this.cache;
            else return getter.apply(owner.getEntity());
        }

        public void setCachedValueUnchecked(Object value) {
            this.cache = value;
        }

        public static enum Direction {
            SERVER_TO_CLIENT, CLIENT_TO_SERVER;

            public boolean isMainSide(boolean trueMeansClient) {
                return this == (trueMeansClient ? CLIENT_TO_SERVER : SERVER_TO_CLIENT);
            }

            public boolean isSynchedSide(boolean trueMeansClient) {
                return !isMainSide(trueMeansClient);
            }

            public boolean isMainSide(LogicalSide side) {
                return isMainSide(side.isClient());
            }

            public boolean isSynchedSide(LogicalSide side) {
                return !isMainSide(side);
            }
        }

    }

    public static record SyncRecord(Map<String, SyncValueEntry> data, Map<String, SyncValueEntry> getters) {
        private static SyncRecord byComponent(EntitySyncherComponent<?> syncher) {
            SyncRecord res = new SyncRecord(new HashMap<>(), new HashMap<>());
            for (String key: syncher.changedDataKeys) {
                res.data.put(key, new SyncValueEntry(syncher.synchedData.get(key).serializer, syncher.synchedData.get(key).value));
            }
            syncher.synchedGetters.entrySet().stream()
                .filter(entry -> entry.getValue().direction.equals(SynchedGetter.Direction.SERVER_TO_CLIENT))
                .forEach(entry -> {
                    res.getters.put(entry.getKey(), new SyncValueEntry(entry.getValue().serializer, entry.getValue().getter.apply(syncher.getEntity())));
                });
            return res;
        }

        private void readBuf(FriendlyByteBuf buf) {
            this.data.putAll(buf.readMap(FriendlyByteBuf::readUtf, SyncValueEntry::read));
            this.getters.putAll(buf.readMap(FriendlyByteBuf::readUtf, SyncValueEntry::read));
        }

        private static SyncRecord fromBuf(FriendlyByteBuf buf) {
            SyncRecord res = new SyncRecord(new HashMap<>(), new HashMap<>());
            res.readBuf(buf);
            return res;
        }

        private void writeBuf(FriendlyByteBuf buf) {
            buf.writeMap(data, FriendlyByteBuf::writeUtf, (b, v) -> v.write(b));
            buf.writeMap(getters, FriendlyByteBuf::writeUtf, (b, v) -> v.write(b));
        }

        public Map<String, Optional<Object>> dataValues() {
            return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Optional.ofNullable(e.getValue().value())));
        }

        public Map<String, Optional<Object>> getterValues() {
            return getters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Optional.ofNullable(e.getValue().value())));
        }

    }

    /**
     * Posted once each level tick, sync all entities in a level.
     */
    public static class ClientboundEntitySyncherComponentSyncAllPacket implements Packet<ClientGamePacketListener> {
        private static final ServerOnly<Long> CURRENT_PACKET_ID = new ServerOnly<>(0L);
        public final long packetId;
        public final ResourceLocation dimension;
        final Map<UUID, Map<HierarchyPath, SyncRecord>> allEntityData = new HashMap<>();
        // Cache components handled so we know which components to clear changed data list
        final Multimap<UUID, HierarchyPath> handledComponentPaths = HashMultimap.create();

        public ClientboundEntitySyncherComponentSyncAllPacket(ServerLevel level, boolean ignoresSyncIntervals) {
            this.packetId = CURRENT_PACKET_ID.get();
            CURRENT_PACKET_ID.set(CURRENT_PACKET_ID.get() + 1);
            this.dimension = level.dimension().location();
            for (Entity e: level.getEntities().getAll()) {
                allEntityData.putIfAbsent(e.getUUID(), new HashMap<>());
                EntityComponentAPI.getComponentManager(e).getAllPathsAndDownstreamComponents()
                    .entrySet().stream()
                    .filter(entry ->
                        entry.getValue() instanceof EntitySyncherComponent<?> sc
                        && sc.shouldSync()
                        && (ignoresSyncIntervals || sc.getEntity().tickCount % sc.getSyncInterval() == 0))
                    .forEach(entry -> {
                        allEntityData.get(e.getUUID()).putIfAbsent(entry.getKey(), SyncRecord.byComponent((EntitySyncherComponent<?>)(entry.getValue())));
                        handledComponentPaths.put(e.getUUID(), entry.getKey());
                    });
            }

        }

        public ClientboundEntitySyncherComponentSyncAllPacket(FriendlyByteBuf buf) {
            this.packetId = buf.readLong();
            this.dimension = buf.readResourceLocation();
            this.allEntityData.putAll(buf.readMap(FriendlyByteBuf::readUUID,
                 buf1 -> buf1.readMap(buf2 -> HierarchyPath.byLiteral(buf2.readUtf()), SyncRecord::fromBuf)));
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeLong(this.packetId);
            buf.writeResourceLocation(this.dimension);
            buf.writeMap(this.allEntityData, FriendlyByteBuf::writeUUID,
                (buf1, map) -> buf1.writeMap(map, (buf2, path) -> buf2.writeUtf(path.toString()),
                    (buf2, rec) -> rec.writeBuf(buf2)));
        }

        @Override
        public void handle(ClientGamePacketListener pHandler) {
            EntityComponentPresetClientPacketHandlers.handleEntitySyncherComponentSyncAll(this, pHandler);
        }
    }

    public static class ClientboundEntitySyncherComponentSyncPacket implements Packet<ClientGamePacketListener> {
        private static final ServerOnly<Long> CURRENT_PACKET_ID = new ServerOnly<>(0L);
        public final long packetId;
        @Nullable   // Non-null on server, null on client
        public final EntitySyncherComponent<? extends Entity> syncher;
        public final int entityID;
        public final UUID entityUUID;
        public final String componentPath;
        public final SyncRecord syncRecord;

        public ClientboundEntitySyncherComponentSyncPacket(EntitySyncherComponent<? extends Entity> syncher) {
            this.packetId = CURRENT_PACKET_ID.get();
            CURRENT_PACKET_ID.set(CURRENT_PACKET_ID.get() + 1);
            this.syncher = syncher;
            this.entityID = syncher.getEntity().getId();
            this.entityUUID = syncher.getEntity().getUUID();
            this.componentPath = syncher.getPathFromRoot().toLiteral();
            this.syncRecord = SyncRecord.byComponent(syncher);
        }

        public ClientboundEntitySyncherComponentSyncPacket(FriendlyByteBuf buf){
            this.packetId = buf.readLong();
            this.syncher = null;
            this.entityID = buf.readInt();
            this.entityUUID = buf.readUUID();
            this.componentPath = buf.readUtf();
            this.syncRecord = SyncRecord.fromBuf(buf);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeLong(this.packetId);
            buf.writeInt(entityID);
            buf.writeUUID(entityUUID);
            buf.writeUtf(componentPath.toString());
            syncRecord.writeBuf(buf);
        }

        @Override
        public void handle(ClientGamePacketListener pHandler) {
            EntityComponentPresetClientPacketHandlers.handleEntitySyncherComponentSync(this, pHandler);
        }

    }
    public static class ServerboundPlayerEntitySyncherComponentSyncPacket implements Packet<ServerGamePacketListener> {

        private static final ClientOnly<Long> CURRENT_PACKET_ID = new ClientOnly<>(0L);
        public final long packetId;
        public final UUID entityUUID;
        @Nullable   // Non-null on server, null on client
        public final EntitySyncherComponent<? extends Entity> syncher;
        public final String componentPath;
        public final Map<String, SyncValueEntry> getters = new HashMap<>();

        public ServerboundPlayerEntitySyncherComponentSyncPacket(EntitySyncherComponent<? extends Entity> syncher) {
            this.packetId = CURRENT_PACKET_ID.get();
            CURRENT_PACKET_ID.set(CURRENT_PACKET_ID.get() + 1);
            this.syncher = syncher;
            this.entityUUID = syncher.getEntity().getUUID();
            this.componentPath = syncher.getPathFromRoot().toLiteral();
            syncher.synchedGetters.entrySet().stream()
                .filter(entry -> entry.getValue().direction.equals(SynchedGetter.Direction.CLIENT_TO_SERVER))
                .forEach(entry -> {
                    this.getters.put(entry.getKey(), new SyncValueEntry(entry.getValue().serializer, entry.getValue().getter.apply(syncher.getEntity())));
                });
        }

        public ServerboundPlayerEntitySyncherComponentSyncPacket(FriendlyByteBuf buf) {
            this.packetId = buf.readLong();
            this.syncher = null;
            this.entityUUID = buf.readUUID();
            this.componentPath = buf.readUtf();
            this.getters.putAll(buf.readMap(FriendlyByteBuf::readUtf, SyncValueEntry::read));
        }

        @Override
        public void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeLong(this.packetId);
            pBuffer.writeUUID(this.entityUUID);
            pBuffer.writeUtf(componentPath);
            pBuffer.writeMap(getters, FriendlyByteBuf::writeUtf, (b, v) -> v.write(b));
        }

        @Override
        public void handle(ServerGamePacketListener pHandler) {
            EntityComponentPresetServerPacketHandlers.HandleEntitySyncherComponentSync(this, pHandler);
        }

        public Map<String, Optional<Object>> getterValues() {
            return getters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Optional.ofNullable(e.getValue().value())));
        }
    }

    public static record SyncValueEntry(@Nullable NFUDataSerializer<?> serializer, @Nullable Object value) {

        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(value != null);
            if (value != null) {
                buf.writeResourceLocation(serializer.getKey());
                ((NFUDataSerializer<Object>)serializer).write(buf, value);
            }
        }

        public static SyncValueEntry read(FriendlyByteBuf buf) {
            if (buf.readBoolean()) {
                NFUDataSerializer<?> s = NFUDataSerializer.fromId(buf.readResourceLocation());
                Object v = s.read(buf);
                return new SyncValueEntry(s, v);
            }
            else return new SyncValueEntry(null, null);
        }
    };

    public static class Default extends EntitySyncherComponent<Entity> {

        public Default(Entity entity) {
            super(entity);
            if (entity instanceof Player) {
                // For NFULevelStatics#getMouseFocus
                this.createSynchedGetter("mouseFocus", NFUDataSerializers.HIT_RESULT_INFO, null,
                    SynchedGetter.Direction.CLIENT_TO_SERVER,
                    e -> Optional.ofNullable(Minecraft.getInstance().hitResult)
                        .map(HitResultInfo::byHitResult)
                        .orElseGet(() -> HitResultInfo.miss(e.position())));
            }
            // For NFUEntityStatics#getMobAttackTarget
            if (entity instanceof Mob mob) {
                this.createSynchedGetter("attackTarget", NFUDataSerializers.UUID, new UUID(0L, 0L),
                    e -> Optional.ofNullable(((Mob)e).getTarget()).map(Entity::getUUID).orElseGet(() -> new UUID(0, 0)));
            }
        }

    }



}
