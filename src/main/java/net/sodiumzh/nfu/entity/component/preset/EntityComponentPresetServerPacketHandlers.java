package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.entity.component.EntityComponentAPI;
import net.sodiumzh.nfu.object.HierarchyPath;

public class EntityComponentPresetServerPacketHandlers {

    public static void HandleEntitySyncherComponentSync(EntitySyncherComponent.ServerboundPlayerEntitySyncherComponentSyncPacket packet, ServerGamePacketListener listener) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(packet.entityUUID);
        if (player == null) return;
        EntitySyncherComponent<? extends Entity> syncher = EntityComponentAPI.getComponentManager(player)
            .getSubComponentByPath(HierarchyPath.byLiteral(packet.componentPath))
            .map(c -> c instanceof EntitySyncherComponent<?> esc ? esc : null).orElse(null);
        if (syncher == null) return;
        if (syncher.lastReceivedPacketId > packet.packetId) return; // This means the packet is out-of-date
        syncher.lastReceivedPacketId = packet.packetId;
        packet.getterValues().forEach((k, v) -> syncher.setSynchedGetterCachedValueOnSynchedSide(k, v.orElse(null)));
    }


}
