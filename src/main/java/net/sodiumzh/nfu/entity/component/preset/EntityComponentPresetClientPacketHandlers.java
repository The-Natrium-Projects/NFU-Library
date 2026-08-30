package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sodiumzh.nfu.entity.component.CEntityComponentManager;
import net.sodiumzh.nfu.entity.component.EntityComponentAPI;
import net.sodiumzh.nfu.object.HierarchyPath;
import net.sodiumzh.nfu.reflection.CachedMethodSearchers;
import net.sodiumzh.nfu.util.NFUDebugStatics;
import net.sodiumzh.nfu.util.NFUEntityStatics;

import java.util.Objects;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class EntityComponentPresetClientPacketHandlers {

    public static void handleEntitySyncherComponentSync(EntitySyncherComponent.ClientboundEntitySyncherComponentSyncPacket packet, ClientGamePacketListener listener) {
        Minecraft mc = Minecraft.getInstance();
        PacketUtils.ensureRunningOnSameThread(packet, listener, mc);
        ClientLevel level = mc.level;
        if (level == null) return;
        Entity e = level.getEntity(packet.entityID);
        if (e == null || !e.getUUID().equals(packet.entityUUID)) {  // Not find or wrong uuid, search from uuid
            if (e == null)
                NFUDebugStatics.warnOnce("Failed to find entity id " + packet.entityID + ", searching from UUID.");
            else {
                NFUDebugStatics.warnOnce("Found wrong entity " + e.getUUID() + " by ID " + packet.entityID + ", expected " + packet.entityUUID + " . Searching from UUID.");
                e = null;
            }
            e = NFUEntityStatics.getEntityByUUID(level, packet.entityUUID);
        }
        if (e == null) {
            NFUDebugStatics.warnOnce("Failed to find entity id " + packet.entityID + " and UUID " + packet.entityUUID + " . Skipped.");
            return;
        }
        EntityComponentAPI.getComponentByPath(e, HierarchyPath.byLiteral(packet.componentPath))
            .filter(c -> c instanceof EntitySyncherComponent<? extends Entity>)
            .map(c -> (EntitySyncherComponent<? extends Entity>)c)
            .ifPresent(c -> {
                if (c.lastReceivedManualPacketId > packet.packetId) return;
                packet.syncRecord.dataValues().forEach((k, v) -> c.setSynchedDataClient(k, v.orElse(null)));
                packet.syncRecord.getterValues().forEach((k, v) -> c.setSynchedGetterCachedValueOnSynchedSide(k, v.orElse(null)));
                c.lastReceivedManualPacketId = packet.packetId;
            });
    }

    public static void handleEntitySyncherComponentSyncAll(EntitySyncherComponent.ClientboundEntitySyncherComponentSyncAllPacket packet, ClientGamePacketListener listener) {
        Minecraft mc = Minecraft.getInstance();
        PacketUtils.ensureRunningOnSameThread(packet, listener, mc);
        ClientLevel level = mc.level;
        if (level == null) return;
        if (!level.dimension().location().equals(packet.dimension)) return;

        for (Entity e: NFUEntityStatics.getLevelEntityGetter(level).getAll()) {
            CEntityComponentManager mgr = EntityComponentAPI.getComponentManager(e);
            Optional.ofNullable(packet.allEntityData.get(e.getUUID()))
                .ifPresent(map -> map.forEach((path, rec) -> {
                    mgr.getSubComponentByPath(path).filter(c -> c instanceof EntitySyncherComponent<? extends Entity>)
                        .map(c -> (EntitySyncherComponent<? extends Entity>)c)
                        .ifPresent(c -> {
                            if (c.lastReceivedPacketId > packet.packetId) return;   // This means the packet is out-of-date
                            rec.dataValues().forEach((k, v) -> c.setSynchedDataClient(k, v.orElse(null)));
                            rec.getterValues().forEach((k, v) -> c.setSynchedGetterCachedValueOnSynchedSide(k, v.orElse(null)));
                            c.lastReceivedPacketId = packet.packetId;
                        });
                }));
        }
    }

}

