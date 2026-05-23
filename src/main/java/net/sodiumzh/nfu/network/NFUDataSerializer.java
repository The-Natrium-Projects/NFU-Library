package net.sodiumzh.nfu.network;

import com.google.common.base.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.sodiumzh.nfu.annotation.DontOverride;
import net.sodiumzh.nfu.registry.NFUEntityDataSerializers;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.registry.NFURegistry;
import net.sodiumzh.nfu.registry.NFURegistryGenerateValuesEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
/**
 * Defines a data type that can serialized both into nbt and FriendlyByteBuf.
 */

public interface NFUDataSerializer<T>
{
	public static NFURegistry<NFUDataSerializer<?>> getRegistry()
	{
		return NFURegistries.DATA_SERIALIZERS;
	}

	public static NFUDataSerializer<?> getFromRegistry(ResourceLocation key)
	{
		return NFURegistries.DATA_SERIALIZERS.getValue(key);
	}

	public Class<T> getObjectClass();
	public ResourceLocation getKey();
	/** Write the data to buf. */
	public void write(FriendlyByteBuf buf, T obj);
	/** Get value from buf. */
	public T read(FriendlyByteBuf buf);
	public Tag toTag(T obj);
	public T fromTag(Tag tag);
	
	/**
	 * Type-unchecked version of {@code write}. Useful if the type is unknown.
	 * Take care and ensure that the object type matches the data type.
	 */
	@SuppressWarnings("unchecked")
	public static <O> void writeUnchecked(NFUDataSerializer<O> type, FriendlyByteBuf buf, Object obj)
	{
		try {
			type.write(buf, (O)obj);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(String.format("NFUDataSerializer#writeUnchecked: cast failed. Attempting to cast \"%s\" to \"%s\".", 
					obj.getClass().getSimpleName(), type.getClass().getSimpleName()), e);
		}
	}
	
	/**
	 * Type-unchecked version of {@code toTag}. Useful if the type is unknown.
	 * Take care and ensure that the object type matches the data type.
	 */
	@SuppressWarnings("unchecked")
	public static <O> Tag toTagUnchecked(NFUDataSerializer<O> type, Object obj)
	{
		try {
			return type.toTag((O)obj);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(String.format("NFUDataSerializer#toTagUnchecked: cast failed. Attempting to cast \"%s\" to \"%s\".", 
					obj.getClass().getSimpleName(), type.getClass().getSimpleName()), e);
		}
	}
	
	public static NFUDataSerializer<?> fromId(ResourceLocation key)
	{
		NFUDataSerializer<?> res = getFromRegistry(key);
		if (res == null)
			throw new IllegalStateException(String.format("NFUDataSerializer: missing serializer \"%s\". If you are using custom data serializers, ensure the related classes are loaded on mod initialization!", 
					key.toString()));
		return res;
	}
	
	// Construction Utils //
	
	/**
	 * Create an instance with serialization/deserialization methods.
	 * <p>Note: {@code NFUDataSerializer} is auto-registered on create. <u>Ensure the class where the
	 * serializers are defined is loaded on mod initialization!</u> 
	 * (E.g. by calling the class owning the instances somehow in the mod main class constructor)
	 */
	public static <O> NFUDataSerializer<O> create(Class<O> objClass,
                                                  BiConsumer<FriendlyByteBuf, O> write, Function<FriendlyByteBuf, O> read,
                                                  Function<O, Tag> toTag, Function<Tag, O> fromTag)
	{
		NFUDataSerializer<O> res = new NFUDataSerializer<O>()
		{
			@Override
			public Class<O> getObjectClass()
			{
				return objClass;
			}
			@Override
			public ResourceLocation getKey()
			{
				return getRegistry().getKey(this);
			}
			@Override
			public void write(FriendlyByteBuf buf, O obj)
			{
				write.accept(buf, obj);
			}
			@Override
			public O read(FriendlyByteBuf buf)
			{
				return read.apply(buf);
			}
			@Override
			public Tag toTag(O obj)
			{
				return toTag.apply(obj);
			}
			@Override
			public O fromTag(Tag t)
			{
				return fromTag.apply(t);
			}
			@Override
			public String toString()
			{
				return String.format("NFUDataSerializer<%s>", this.getKey().toString());
			}
		};
		return res;
	}
	
	@SuppressWarnings("unchecked")
	public static <O, T extends Tag> NFUDataSerializer<O> create(Class<O> objClass, Class<T> tagClass,
                                                                 BiConsumer<FriendlyByteBuf, O> write, Function<FriendlyByteBuf, O> read,
                                                                 Function<O, T> toTag, Function<T, O> fromTag)
	{
		return create(objClass, write, read, o -> toTag.apply(o), t -> fromTag.apply((T)t));
	}
	
	/**
	 * Create a list serializer from element serializer.
	 * <p>Note: {@code NFUDataSerializer} is auto-registered on create. <u>Ensure the class where the
	 * serializers are defined is loaded on mod initialization!</u> 
	 * (E.g. by calling the class owning the instances somehow in the mod main class constructor)
	 */
	@SuppressWarnings("unchecked")
	public static <O> NFUDataSerializer<List<O>> createList(final NFUDataSerializer<O> original)
	{
		Class<?> clazz = List.class;
		return NFUDataSerializer.create((Class<List<O>>)clazz, ListTag.class,
				(b, o) -> {
					b.writeInt(o.size());
					for (int i = 0; i < o.size(); ++i)
						original.write(b, o.get(i));
				}, (b) -> {
					int size = b.readInt();
					List<O> list = new ArrayList<>();
					for (int i = 0; i < size; ++i)
						list.add(original.read(b));
					return list;
				}, (o) -> {
					ListTag t = new ListTag();
					for (int i = 0; i < o.size(); ++i)
						t.add(original.toTag(o.get(i)));
					return t;
				}, (t) -> {
					List<O> list = new ArrayList<>();
					for (int i = 0; i < t.size(); ++i)
						list.add(original.fromTag(t.get(i)));
					return list;
				});
	}
	
	public static <A, B> NFUDataSerializer<B> castTo(Class<B> clazzB,
                                                     NFUDataSerializer<A> original, Function<A, B> aToB, Function<B, A> bToA)
	{
		return NFUDataSerializer.create(clazzB,
				(buf, b) -> original.write(buf, bToA.apply(b)),
				(buf) -> aToB.apply(original.read(buf)),
				b -> original.toTag(bToA.apply(b)),
				t -> aToB.apply(original.fromTag(t)));
	}

	@SuppressWarnings("unchecked")
	public static <O> NFUDataSerializer<Optional<O>> createOptional(final NFUDataSerializer<O> original) {
		Class<?> clazz = Optional.class;
		return NFUDataSerializer.create((Class<Optional<O>>)clazz, CompoundTag.class,
			(FriendlyByteBuf b, Optional<O> optional) -> {
				optional.ifPresentOrElse(o -> {
					b.writeBoolean(true);
					original.write(b, o);
				}, () -> b.writeBoolean(false));
			}, (FriendlyByteBuf b) -> {
				if (b.readBoolean())
					return Optional.of(original.read(b));
				else return Optional.empty();
			}, (Optional<O> optional) -> {
				CompoundTag res = new CompoundTag();
				optional.ifPresent(o -> res.put("value", original.toTag(o)));
				return res;
			}, (CompoundTag t) -> {
				if (t.contains("value"))
					return Optional.of(original.fromTag(t.get("value")));
				else return Optional.empty();
			});
	}

	/**
	 * Get the optional variant of this serializer. It's automatically created after loading the registry (via {@link net.sodiumzh.nfu.eventhandler.NFUSetupEventHandlers#onGenerateRegistries(NFURegistryGenerateValuesEvent.CommonAfter)}).
	 */
	@ApiStatus.NonExtendable
	public default NFUDataSerializer<Optional<T>> getOptionalSerializer() {
		if (this.getObjectClass().equals(List.class) && this.getKey().getPath().endsWith("_list") ) {
			throw new IllegalStateException("List serializer doesn't have its optional serializer. Serializer: " + this.getKey());
		}
		if (this.getObjectClass().equals(Optional.class) && this.getKey().getPath().startsWith("optional_")) {
			throw new IllegalStateException("Optional serializer doesn't have its optional serializer. Serializer: " + this.getKey());
		}
		return (NFUDataSerializer<Optional<T>>) NFURegistries.DATA_SERIALIZERS.getOptionalValue(new ResourceLocation(this.getKey().getNamespace(), "optional_" + this.getKey().getPath()))
			.filter(s -> s.getObjectClass().equals(Optional.class)).orElseThrow(() -> new IllegalStateException("Missing optional serializer " + new ResourceLocation(this.getKey().getNamespace(), "optional_" + this.getKey().getPath()).toString() + ". An existing manually-registered non-optional serializer?"));
	}

	/**
	 * Get the list variant of this serializer from registry. It's automatically created after loading the registry (via {@link net.sodiumzh.nfu.eventhandler.NFUSetupEventHandlers#onGenerateRegistries(NFURegistryGenerateValuesEvent.CommonAfter)}).
	 */
	@ApiStatus.NonExtendable
	public default NFUDataSerializer<List<T>> getListSerializer() {
		if (this.getObjectClass().equals(List.class) && this.getKey().getPath().endsWith("_list") ) {
			throw new IllegalStateException("List serializer doesn't have its list serializer. Serializer: " + this.getKey());
		}
		if (this.getObjectClass().equals(Optional.class) && this.getKey().getPath().startsWith("optional_")) {
			throw new IllegalStateException("Optional serializer doesn't have its list serializer. Serializer: " + this.getKey());
		}
		return (NFUDataSerializer<List<T>>) NFURegistries.DATA_SERIALIZERS.getOptionalValue(new ResourceLocation(this.getKey().getNamespace(), this.getKey().getPath() + "_list"))
			.filter(s -> s.getObjectClass().equals(List.class)).orElseThrow(() -> new IllegalStateException("Missing list serializer " + this.getKey().toString() + "_list. An existing manually-registered non-optional serializer?"));
	}

	@DontOverride
	public default EntityDataSerializer<T> asEntityDataSerializer() {
		return NFUEntityDataSerializers.create(this::write, this::read);
	}

}
