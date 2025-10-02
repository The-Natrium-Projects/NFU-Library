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
import net.sodiumzh.nfu.container.Tuple2;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
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
		if (target.level.isClientSide)
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
	 * Write a collection to bytebuf.
	 * @param writer Method to write a single element to buf.
	 */
	public static <T> void writeCollection(FriendlyByteBuf buf, Collection<T> collection, BiConsumer<FriendlyByteBuf, T> writer) {
		buf.writeInt(collection.size());
		collection.forEach(t -> writer.accept(buf, t));
	}

	/**
	 * Write a map to bytebuf.
	 * @param keyWriter Method to write a single key to buf.
	 * @param valWriter Method to write a single value to buf.
	 */
	public static <K, V> void writeMap(
		FriendlyByteBuf buf,
		Map<K, V> map,
		BiConsumer<FriendlyByteBuf, K> keyWriter,
		BiConsumer<FriendlyByteBuf, V> valWriter)
	{
		buf.writeInt(map.keySet().size());
		map.forEach((k, v) -> {
			keyWriter.accept(buf, k);
			valWriter.accept(buf, v);
		});
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
			writeCollection(buf, multimap.get(k), valWriter);
		});
	}

	/**
	 * Read a set from bytebuf. Only reads data written by {@link NFUNetworkStatics#writeCollection}.
	 * @param reader Method to read a single element.
	 */
	public static <T> Set<T> readSet(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader) {
		int size = buf.readInt();
		return IntStream.range(0, size).mapToObj(i -> reader.apply(buf)).collect(Collectors.toSet());
	}

	/**
	 * Read a list from bytebuf. Only reads data written by {@link NFUNetworkStatics#writeCollection}.
	 * @param reader Method to read a single element.
	 */
	public static <T> List<T> readList(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader) {
		int size = buf.readInt();
		return IntStream.range(0, size).mapToObj(i -> reader.apply(buf)).collect(Collectors.toList());
	}

	/**
	 * Read a map from bytebuf. Only reads data written by {@link NFUNetworkStatics#writeMap}.
	 * @param keyReader Method to read a single key.
	 * @param valueReader Method to read a single value.   
	 */
	public static <K, V> Map<K, V> readMap(
		FriendlyByteBuf buf, 
		Function<FriendlyByteBuf, K> keyReader,
		Function<FriendlyByteBuf, V> valueReader) 
	{
		int size = buf.readInt();
		Map<K, V> res = new HashMap<>();
		IntStream.range(0, size).forEach(i -> {
			K k = keyReader.apply(buf);
			V v = valueReader.apply(buf);
			res.put(k, v);
		});
		return res;
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
			List<V> vs = readList(buf, valueReader);
			res.putAll(k, vs);
		});
		return res;
	}
	
}
