package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sodiumzh.nfu.entity.component.EntityComponentAPI;
import net.sodiumzh.nfu.object.HierarchyPath;
import net.sodiumzh.nfu.util.NFUDebugStatics;
import net.sodiumzh.nfu.util.NFUEntityStatics;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EntityComponentPresetClientPacketHandlers {

    public static void HandleEntitySyncherComponentSync(EntitySyncherComponent.ClientboundEntitySyncherComponentSyncPacket packet, ClientGamePacketListener listener) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        // Find and verify entity
        @Nullable Entity e = level.getEntity(packet.entityID);    // First get from ID as it's faster
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
        EntitySyncherComponent<? extends Entity> syncher = EntityComponentAPI.getComponentManager(e)
            .getSubComponentByPath(HierarchyPath.byLiteral(packet.componentPath))
            .map(c -> c instanceof EntitySyncherComponent<?> esc ? esc : null).orElse(null);
        if (syncher == null) return;
        if (syncher.lastReceivedPacketId > packet.packetId) return; // This means the packet is out-of-date
        syncher.lastReceivedPacketId = packet.packetId;
        packet.dataValues().forEach((k, v) -> syncher.setSynchedDataClient(k, v.orElse(null)));
        packet.getterValues().forEach((k, v) -> syncher.setSynchedGetterCachedValueOnSynchedSide(k, v.orElse(null)));
    }

}

