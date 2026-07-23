package net.sodiumzh.nfu.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class NFUClientboundPacketHandlers {

    public static void handleEntityMotionUpdate(ClientboundEntityMotionUpdatePacket packet, ClientGamePacketListener listener) {
        Minecraft mc = Minecraft.getInstance();
        PacketUtils.ensureRunningOnSameThread(packet, listener, mc);
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(packet.getId());
        if (e == null) return;
        e.setPos(e.position().add(packet.getDeltaPos()));
        e.addDeltaMovement(packet.getDeltaVelocity());
    }

    public static void handleLivingSyncEquipment(ClientboundLivingSyncEquipmentPacket packet, ClientGamePacketListener listener) {
        Minecraft mc = Minecraft.getInstance();
        PacketUtils.ensureRunningOnSameThread(packet, listener, mc);
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(packet.entityID);
        if (!(e instanceof LivingEntity le)) return;
        List<EquipmentSlot> slots = Arrays.stream(EquipmentSlot.values()).sorted(Comparator.comparingInt(EquipmentSlot::getFilterFlag)).toList();
        int size = Math.min(slots.size(), packet.items.size());
        for (int i = 0; i < size; ++i) {
            le.setItemSlot(slots.get(i), packet.items.get(i));
        }
    }

}
