package net.sodiumzh.nfu.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public class NFUNetworkStatics
{
	
	public static SimpleChannel newChannel(String modId, String key, String version)
	{
		return NetworkRegistry.newSimpleChannel(
                new ResourceLocation(modId, key),
                () -> {
                    return version;
                },
                (v) -> {
                    return v.equals(version);
                },
                (v) -> {
                    return v.equals(version);
                });
	}
	
	public static SimpleChannel newChannel(String modId, String key)
	{
		return newChannel(modId, key, "1.0");
	}

	/**
	 * Register a default game packet sent from server to client player(s). The packet must have a
	 * constructor receiving only one argument of {@link FriendlyByteBuf}.
	 */
	public static <T extends Packet<ClientGamePacketListener>>
			void registerDefaultClientGamePacket(int id, SimpleChannel channel, Class<T> packetClass)
	{
		channel.registerMessage(id, packetClass, Packet::write,
			(buffer) -> {
				try
				{
					return packetClass.getConstructor(FriendlyByteBuf.class).newInstance(buffer);
				}
				catch (Exception e)
				{
					throw new IllegalArgumentException("NFUNetworkStatics::registerDefaultClientGamePacket packet class missing constructor.", e);
				}
			},
			(pack, ctx) -> {
				ctx.get().enqueueWork(() ->
					Optional.ofNullable(Minecraft.getInstance().getConnection()).ifPresent(pack::handle));
				ctx.get().setPacketHandled(true);
			}
		);
	}

	/**
	 * Register a default game packet sent from client player to server. The packet must have a
	 * constructor receiving only one argument of {@link FriendlyByteBuf}.
	 */
	public static <T extends Packet<ServerGamePacketListener>>
		void registerDefaultServerGamePacket(int id, SimpleChannel channel, Class<T> packetClass)
	{
		channel.registerMessage(id,packetClass, Packet::write, buffer -> {
				try {
					return packetClass.getConstructor(FriendlyByteBuf.class).newInstance(buffer);
				} catch (Exception e) {
					throw new IllegalArgumentException("NFUNetworkStatics::registerDefaultServerGamePacket packet class missing constructor.", e);
				}
			}, (pack, ctx) -> {
				ctx.get().enqueueWork(() ->
					Optional.ofNullable(ctx.get().getSender()).map(p -> p.connection).ifPresent(pack::handle));
				ctx.get().setPacketHandled(true);
			}
		);
	}

	/**
	 * Send a packet from server to a given player.
	 */
	public static void sendToPlayer(SimpleChannel channel, Packet<?> msg, Player target)
	{
		if (target.level().isClientSide)
			return;
		if (target instanceof ServerPlayer sp)
		{
			channel.send(PacketDistributor.PLAYER.with(() -> sp), msg);
		}
	}
	
	/**
	 * Send packet from server to all players in a level.
	 */
	public static void sendToAllPlayers(Level level, SimpleChannel channel, Packet<?> message)
	{
		if (level.isClientSide)
			return;
		for (Player player: level.players())
		{
			if (player instanceof ServerPlayer sp)
			{
				channel.send(PacketDistributor.PLAYER.with(() -> sp), message);
			}
		}
	}

	public static void sendToServer(Player player, SimpleChannel channel, Packet<?> message) {
		if (!player.level().isClientSide) return;
		channel.sendToServer(message);
	}

	/**
	 * Write a multimap to bytebuf.
	 * @param keyWriter Method to write a single key to buf.
	 * @param valWriter Method to write a single value to buf.
	 */
	public static <K, V> void writeMultimap(
		FriendlyByteBuf buf,
		Multimap<K, V> multimap,
		BiConsumer<FriendlyByteBuf, K> keyWriter,
		BiConsumer<FriendlyByteBuf, V> valWriter) 
	{
		buf.writeInt(multimap.keySet().size());
		multimap.keySet().forEach(k -> {
			keyWriter.accept(buf, k);
			buf.writeCollection(multimap.get(k), valWriter::accept);
		});
	}

	/**
	 * Read a multimap from bytebuf. Only reads data written by {@link NFUNetworkStatics#writeMultimap}.
	 * @param keyReader Method to read a single key.
	 * @param valueReader Method to read a single value.   
	 */
	public static <K, V> Multimap<K, V> readMultimap(
		FriendlyByteBuf buf,
		Function<FriendlyByteBuf, K> keyReader,
		Function<FriendlyByteBuf, V> valueReader)
	{
		int size = buf.readInt();
		Multimap<K, V> res = HashMultimap.create();
		IntStream.range(0, size).forEach(i -> {
			K k = keyReader.apply(buf);
			List<V> vs = buf.readList(valueReader::apply);
			res.putAll(k, vs);
		});
		return res;
	}
	
}
