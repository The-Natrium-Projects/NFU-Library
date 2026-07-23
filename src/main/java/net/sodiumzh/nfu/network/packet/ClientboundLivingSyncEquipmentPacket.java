package net.sodiumzh.nfu.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ClientboundLivingSyncEquipmentPacket implements Packet<ClientGamePacketListener> {

    final int entityID;
    final List<ItemStack> items;

    public ClientboundLivingSyncEquipmentPacket(LivingEntity l) {
        this.entityID = l.getId();
        this.items = Arrays.stream(EquipmentSlot.values()).sorted(Comparator.comparingInt(EquipmentSlot::getFilterFlag))
            .map(l::getItemBySlot).toList();
    }

    public ClientboundLivingSyncEquipmentPacket(FriendlyByteBuf buf) {
        this.entityID = buf.readInt();
        this.items = buf.readList(FriendlyByteBuf::readItem);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeCollection(this.items, FriendlyByteBuf::writeItem);
    }

    @Override
    public void handle(ClientGamePacketListener pHandler) {
        NFUClientboundPacketHandlers.handleLivingSyncEquipment(this, pHandler);
    }
}
