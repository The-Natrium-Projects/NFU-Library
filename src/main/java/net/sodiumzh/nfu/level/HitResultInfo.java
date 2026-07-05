package net.sodiumzh.nfu.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.datafix.fixes.EntityUUIDFix;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sodiumzh.nfu.network.NFUDataSerializer;
import net.sodiumzh.nfu.network.NFUDataSerializers;
import net.sodiumzh.nfu.util.NFUDebugStatics;
import net.sodiumzh.nfu.util.NFUEntityStatics;
import net.sodiumzh.nfu.util.NFUMathStatics;
import org.checkerframework.checker.units.qual.C;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.UUID;

/**
 * A context-independent serializable record carrying information of a hit result.
 */
public class HitResultInfo {

    public final HitResult.Type type;
    public final Vec3 location;
    // For block hit result only
    @Nullable
    private final BlockHitResult blockHitResult;
    @Nullable
    private final Direction direction;
    @Nullable
    private final BlockPos blockPos;
    private final boolean inside;
    // For entity hit result only
    @Nullable
    private final UUID entityUUID;

    private HitResultInfo(HitResult hitResult) {
        this.location = hitResult.getLocation();
        if (hitResult instanceof BlockHitResult bhr) {
            this.type = hitResult.getType();
            this.blockHitResult = bhr;
            this.direction = bhr.getDirection();
            this.blockPos = bhr.getBlockPos();
            this.inside = bhr.isInside();
            this.entityUUID = null;
        } else if (hitResult instanceof EntityHitResult ehr) {
            this.type = hitResult.getType();
            this.blockHitResult = null;
            this.entityUUID = ehr.getEntity().getUUID();
            this.direction = null;
            this.blockPos = null;
            this.inside = false;
        } else {
            NFUDebugStatics.errorOnce("HitResultInfo: only supports vanilla hit result types. Regarded as MISS.");
            this.type = HitResult.Type.MISS;
            this.direction = Direction.getNearest(location.x, location.y, location.z);
            this.blockPos = NFUMathStatics.getBlockPos(this.location);
            this.blockHitResult = BlockHitResult.miss(location, direction, blockPos);
            this.inside = false;
            this.entityUUID = null;
        }
    }

    private HitResultInfo(UUID uuid, Vec3 location) {
        this.location = location;
        this.type = HitResult.Type.ENTITY;
        this.blockHitResult = null;
        this.direction = null;
        this.blockPos = null;
        this.inside = false;
        this.entityUUID = uuid;
    }

    public static HitResultInfo miss(Vec3 location) {
        Direction direction = Direction.getNearest(location.x, location.y, location.z);
        BlockPos blockPos = NFUMathStatics.getBlockPos(location);
        return new HitResultInfo(BlockHitResult.miss(location, direction, blockPos) );
    }

    public static HitResultInfo byHitResult(HitResult hitResult) {
        return new HitResultInfo(hitResult);
    }

    public static HitResultInfo byEntityUUID(UUID uuid, Vec3 location) {
        return new HitResultInfo(uuid, location);
    }

    public HitResult toHitResult(Level context) {
        return switch (this.type) {
            case MISS -> BlockHitResult.miss(this.location, this.direction, this.blockPos);
            case BLOCK -> new BlockHitResult(this.location, this.direction, this.blockPos, this.inside);
            case ENTITY ->
                new EntityHitResult(NFUEntityStatics.getEntityByUUID(context, this.entityUUID), this.location);
        };
    }

    public void writeBuf(FriendlyByteBuf buf) {
        boolean isBlock = !this.type.equals(HitResult.Type.ENTITY);
        buf.writeBoolean(isBlock);
        if (isBlock) {
            buf.writeBlockHitResult(this.blockHitResult);
        }
        else {
            buf.writeUUID(this.entityUUID);
            NFUDataSerializers.VEC3.write(buf, this.location);
        }
    }

    public static HitResultInfo readBuf(FriendlyByteBuf buf) {
        boolean isBlock = buf.readBoolean();
        if (isBlock) return HitResultInfo.byHitResult(buf.readBlockHitResult());
        else return HitResultInfo.byEntityUUID(buf.readUUID(), NFUDataSerializers.VEC3.read(buf));
    }

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isBlock", !this.type.equals(HitResult.Type.ENTITY));
        nbt.put("location", NFUDataSerializers.VEC3.toTag(this.location));
        if (this.type.equals(HitResult.Type.ENTITY)) {
            nbt.putUUID("entityUUID", this.entityUUID);
        } else {
            nbt.putBoolean("isMiss", this.type.equals(HitResult.Type.MISS));
            nbt.putString("direction", this.direction.getName());
            nbt.put("blockPos", NFUDataSerializers.BLOCK_POS.toTag(this.blockPos));
            nbt.putBoolean("inside", this.inside);
        }
        return nbt;
    }

    public static HitResultInfo fromNBT(CompoundTag nbt) {
        boolean isBlock = nbt.getBoolean("isBlock");
        Vec3 location = NFUDataSerializers.VEC3.fromTag(nbt.get("location"));
        if (isBlock) {
            boolean isMiss = nbt.getBoolean("isMiss");
            Direction direction = Direction.byName(nbt.getString("direction"));
            boolean inside = nbt.getBoolean("inside");
            BlockPos blockPos = NFUDataSerializers.BLOCK_POS.fromTag(nbt.get("blockPos"));
            BlockHitResult blockHitResult;
            if (isMiss) blockHitResult = BlockHitResult.miss(location, direction, blockPos);
            else blockHitResult = new BlockHitResult(location, direction, blockPos, inside);
            return HitResultInfo.byHitResult(blockHitResult);
        }
        else {
            UUID uuid = nbt.getUUID("entityUUID");
            return HitResultInfo.byEntityUUID(uuid, location);
        }
    }



}
