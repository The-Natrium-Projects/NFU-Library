package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sodiumzh.nfu.entity.component.EntityComponentAPI;

@OnlyIn(Dist.CLIENT)
public class EntityComponentPresetClientPacketHandlers {

    public static void HandleEntitySyncherComponentSync(EntitySyncherComponent.ClientboundEntitySyncherComponentSyncPacket packet, ClientGamePacketListener listener) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity e = level.getEntity(packet.entityID);
        if (e == null) return;
        EntitySyncherComponent<? extends Entity> syncher = EntityComponentAPI.getComponentManager(e)
            .getSubComponentByPath(packet.componentPath)
            .map(c -> c instanceof EntitySyncherComponent<?> esc ? esc : null).orElse(null);
        if (syncher == null) return;
        if (syncher.lastReceivedPacketId > packet.packetId) return; // This means the packet is out-of-date
        syncher.lastReceivedPacketId = packet.packetId;
        packet.dataValues().forEach(syncher::setSynchedDataClient);
        packet.getterValues().forEach(syncher::setSynchedGetterClient);
    }

}

