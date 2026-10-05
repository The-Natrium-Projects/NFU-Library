package net.sodiumzh.nfu.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.level.HitResultInfo;
import net.sodiumzh.nfu.math.*;
import net.sodiumzh.nfu.registry.NFURegistries;
import net.sodiumzh.nfu.registry.NFURegistry;
import net.sodiumzh.nfu.registry.NFURegistryEntryCollection;

import java.util.Optional;
import java.util.UUID;

public class NFUDataSerializers {
    public static NFURegistryEntryCollection<NFUDataSerializer<?>> SERIALIZERS =
            NFURegistryEntryCollection.create(NFURegistries.DATA_SERIALIZERS, NFULibrary.MOD_ID);

    public static final NFURegistry.Accessor<NFUDataSerializer<Boolean>> BOOLEAN = SERIALIZERS.register("boolean", () ->
            NFUDataSerializer.create(
                    Boolean.class, ByteTag.class,
                    FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean, ByteTag::valueOf, t -> t.getAsByte() != 0));
    public static final NFURegistry.Accessor<NFUDataSerializer<Integer>> INT = SERIALIZERS.register("int", () ->
            NFUDataSerializer.create(
                    Integer.class, IntTag.class,
                    FriendlyByteBuf::writeInt, FriendlyByteBuf::readInt, IntTag::valueOf, IntTag::getAsInt));
    public static final NFURegistry.Accessor<NFUDataSerializer<Long>> LONG = SERIALIZERS.register("long", () ->
            NFUDataSerializer.create(
                    Long.class, LongTag.class,
                    FriendlyByteBuf::writeLong, FriendlyByteBuf::readLong, LongTag::valueOf, LongTag::getAsLong));
    public static final NFURegistry.Accessor<NFUDataSerializer<Double>> DOUBLE = SERIALIZERS.register("double", () ->
            NFUDataSerializer.create(
                    Double.class, DoubleTag.class,
                    FriendlyByteBuf::writeDouble, FriendlyByteBuf::readDouble, DoubleTag::valueOf, DoubleTag::getAsDouble));
    public static final NFURegistry.Accessor<NFUDataSerializer<java.util.UUID>> UUID = SERIALIZERS.register("uuid", () ->
            NFUDataSerializer.create(
                    UUID.class, IntArrayTag.class,
                    FriendlyByteBuf::writeUUID, FriendlyByteBuf::readUUID, NbtUtils::createUUID, NbtUtils::loadUUID));
    public static final NFURegistry.Accessor<NFUDataSerializer<String>> STRING = SERIALIZERS.register("string", () ->
            NFUDataSerializer.create(
                    String.class, StringTag.class,
                    FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf, StringTag::valueOf, StringTag::getAsString));
    public static final NFURegistry.Accessor<NFUDataSerializer<ResourceLocation>> RESOURCE_LOCATION = SERIALIZERS.register("resource_location", () ->
            NFUDataSerializer.castTo(
                    ResourceLocation.class,
                    STRING.get(), ResourceLocation::new, ResourceLocation::toString));
    public static final NFURegistry.Accessor<NFUDataSerializer<int[]>> INT_ARRAY = SERIALIZERS.register("int_array", () ->
            NFUDataSerializer.create(
                    int[].class, IntArrayTag.class,
                    (b, o) -> {
                        b.writeInt(o.length);
                        for (int i = 0; i < o.length; ++i)
                            b.writeInt(o[i]);
                    }, b -> {
                        int l = b.readInt();
                        int[] res = new int[l];
                        for (int i = 0; i < l; ++i)
                            res[i] = b.readInt();
                        return res;
                    }, IntArrayTag::new, IntArrayTag::getAsIntArray));
    public static final NFURegistry.Accessor<NFUDataSerializer<double[]>> DOUBLE_ARRAY = SERIALIZERS.register("double_array", () ->
            NFUDataSerializer.create(
                    double[].class, ListTag.class,
                    (b, o) -> {
                        b.writeInt(o.length);
                        for (int i = 0; i < o.length; ++i)
                            b.writeDouble(o[i]);
                    }, b -> {
                        int l = b.readInt();
                        double[] res = new double[l];
                        for (int i = 0; i < l; ++i)
                            res[i] = b.readDouble();
                        return res;
                    }, o -> {
                        ListTag tag = new ListTag();
                        for (int i = 0; i < o.length; ++i)
                            tag.add(DoubleTag.valueOf(o[i]));
                        return tag;
                    }, t -> {
                        double[] res = new double[t.size()];
                        for (int i = 0; i < t.size(); ++i)
                            res[i] = t.getDouble(i);
                        return res;
                    }));
    public static final NFURegistry.Accessor<NFUDataSerializer<Vec3>> VEC3 = SERIALIZERS.register("vec3", () ->
            NFUDataSerializer.create(
                    Vec3.class, ListTag.class,
                    (b, o) -> {b.writeDouble(o.x); b.writeDouble(o.y); b.writeDouble(o.z);},
                    (b) -> new Vec3(b.readDouble(), b.readDouble(), b.readDouble()),
                    (o) -> {
                        ListTag listtag = new ListTag();
                        listtag.add(DoubleTag.valueOf(o.x));
                        listtag.add(DoubleTag.valueOf(o.y));
                        listtag.add(DoubleTag.valueOf(o.z));
                        return listtag;
                    }, (t) -> new Vec3(t.getDouble(0), t.getDouble(1), t.getDouble(2))));
    public static final NFURegistry.Accessor<NFUDataSerializer<LinearColor>> LINEAR_COLOR = SERIALIZERS.register("linear_color", () ->
            NFUDataSerializer.castTo(
                    LinearColor.class, VEC3.get(),
                    LinearColor::fromNormalized, c -> new Vec3(c.r, c.g, c.b)));
    public static final NFURegistry.Accessor<NFUDataSerializer<ItemStack>> ITEM_STACK = SERIALIZERS.register("item_stack", () ->
            NFUDataSerializer.create(
                    ItemStack.class, CompoundTag.class,
                    FriendlyByteBuf::writeItem, FriendlyByteBuf::readItem,
                    (i) -> {CompoundTag res = new CompoundTag(); i.save(res); return res;},
                    ItemStack::of));
    public static final NFURegistry.Accessor<NFUDataSerializer<ItemStack>> ITEM_STACK_FULL_TAG = SERIALIZERS.register("item_stack_full_tag", () ->
            NFUDataSerializer.create(
                    ItemStack.class, CompoundTag.class,
                    (b, i) -> b.writeItemStack(i, false), FriendlyByteBuf::readItem,
                    (i) -> {CompoundTag res = new CompoundTag(); i.save(res); return res;},
                    ItemStack::of));

    public static final NFURegistry.Accessor<NFUDataSerializer<RangedRandomDouble>> RANGED_RANDOM_DOUBLE =
            SERIALIZERS.register("ranged_random_double", () ->
                    NFUDataSerializer.castTo(RangedRandomDouble.class, DOUBLE_ARRAY.get(),
                            RangedRandomDouble::fromArrayRepresentation, RangedRandomDouble::toArrayRepresentation));

    public static final NFURegistry.Accessor<NFUDataSerializer<RangedRandomInt>> RANGED_RANDOM_INT =
            SERIALIZERS.register("ranged_random_int", () ->
                    NFUDataSerializer.castTo(RangedRandomInt.class, DOUBLE_ARRAY.get(),
                        RangedRandomInt::fromArrayRepresentation, RangedRandomInt::toArrayRepresentation));

    public static final NFURegistry.Accessor<NFUDataSerializer<AABB>> BOUNDING_BOX =
            SERIALIZERS.register("bounding_box", () ->
                    NFUDataSerializer.castTo(AABB.class, DOUBLE_ARRAY.get(),
                        o -> new AABB(o[0], o[1], o[2], o[3], o[4], o[5]),
                        o -> new double[]{o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ}));

    public static final NFURegistry.Accessor<NFUDataSerializer<Inequality3D>> INEQUALITY_3D =
        SERIALIZERS.register("inequality_3d", () -> NFUDataSerializer.create(Inequality3D.class, CompoundTag.class,
            (b, i) -> {
                b.writeUtf(IInequalityPattern3D.REGISTRY.getKey(i.pattern).toString());
                VEC3.get().write(b, i.scale);
                VEC3.get().write(b, i.translation);
                BOUNDING_BOX.get().write(b, i.defDomain);
            }, b -> new Inequality3D(IInequalityPattern3D.REGISTRY.getValue(new ResourceLocation(b.readUtf())),
                VEC3.get().read(b), VEC3.get().read(b), BOUNDING_BOX.get().read(b)),
            i -> {
                CompoundTag res = new CompoundTag();
                res.putString("pattern", IInequalityPattern3D.REGISTRY.getKey(i.pattern).toString());
                res.put("scale", VEC3.get().toTag(i.scale));
                res.put("translation", VEC3.get().toTag(i.translation));
                res.put("defDomain", BOUNDING_BOX.get().toTag(i.defDomain));
                return res;
            }, t -> new Inequality3D(IInequalityPattern3D.REGISTRY.getValue(new ResourceLocation(t.getString("pattern"))),
                VEC3.get().fromTag(t.get("scale")), VEC3.get().fromTag(t.get("translation")), BOUNDING_BOX.get().fromTag(t.get("defDomain")))
        ));

    public static final NFURegistry.Accessor<NFUDataSerializer<Optional<Inequality3D>>> OPTIONAL_INEQUALITY_3D =
        SERIALIZERS.register("optional_inequality_3d", () -> NFUDataSerializer.createOptional(INEQUALITY_3D.get()));

    public static final NFURegistry.Accessor<NFUDataSerializer<Field3D>> FIELD_3D =
        SERIALIZERS.register("field_3d", () -> NFUDataSerializer.create(Field3D.class, CompoundTag.class,
            (b, i) -> {
                b.writeUtf(IFieldPattern3D.REGISTRY.getKey(i.pattern).toString());
                VEC3.get().write(b, i.spaceScale);
                VEC3.get().write(b, i.valueScale);
                VEC3.get().write(b, i.translation);
                VEC3.get().write(b, i.valueAddition);
                OPTIONAL_INEQUALITY_3D.get().write(b, Optional.ofNullable(i.baseDefinitionDomain));
            }, b -> new Field3D(IFieldPattern3D.REGISTRY.getValue(new ResourceLocation(b.readUtf())),
                VEC3.get().read(b), VEC3.get().read(b), VEC3.get().read(b), VEC3.get().read(b), OPTIONAL_INEQUALITY_3D.get().read(b).orElse(null)),
            i -> {
                CompoundTag res = new CompoundTag();
                res.putString("pattern", IFieldPattern3D.REGISTRY.getKey(i.pattern).toString());
                res.put("spaceScale", VEC3.get().toTag(i.spaceScale));
                res.put("valueScale", VEC3.get().toTag(i.valueScale));
                res.put("translation", VEC3.get().toTag(i.translation));
                res.put("valueAddition", VEC3.get().toTag(i.valueAddition));
                res.put("defDomain", OPTIONAL_INEQUALITY_3D.get().toTag(Optional.ofNullable(i.baseDefinitionDomain)));
                return res;
            }, t -> new Field3D(IFieldPattern3D.REGISTRY.getValue(new ResourceLocation(t.getString("pattern"))),
                VEC3.get().fromTag(t.get("spaceScale")), VEC3.get().fromTag(t.get("valueScale")), VEC3.get().fromTag(t.get("translation")),
                VEC3.get().fromTag(t.get("valueAddition")), OPTIONAL_INEQUALITY_3D.get().fromTag(t.get("defDomain")).orElse(null))
    ));

    public static final NFURegistry.Accessor<NFUDataSerializer<Optional<Field3D>>> OPTIONAL_FIELD_3D =
        SERIALIZERS.register("optional_field_3d", () -> NFUDataSerializer.createOptional(FIELD_3D.get()));

    public static final NFURegistry.Accessor<NFUDataSerializer<BlockPos>> BLOCK_POS =
        SERIALIZERS.register("block_pos", () -> NFUDataSerializer.create(BlockPos.class, IntArrayTag.class,
            FriendlyByteBuf::writeBlockPos,
            FriendlyByteBuf::readBlockPos,
            o -> new IntArrayTag(new int[]{o.getX(), o.getY(), o.getZ()}),
            (IntArrayTag t) -> new BlockPos(t.getAsIntArray()[0], t.getAsIntArray()[1], t.getAsIntArray()[2])
        ));

    public static final NFURegistry.Accessor<NFUDataSerializer<HitResultInfo>> HIT_RESULT_INFO =
        SERIALIZERS.register("hit_result_info", () -> NFUDataSerializer.create(HitResultInfo.class, CompoundTag.class,
            (buf, hri) -> hri.writeBuf(buf),
            HitResultInfo::readBuf,
            HitResultInfo::toNBT,
            HitResultInfo::fromNBT
        ));
}
