package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Cancelable;
import net.sodiumzh.nfu.event.NFUEntityEvent;

@Cancelable
public class EntityStuckInBlockEvent extends NFUEntityEvent<Entity> {

    private final BlockState blockState;
    private final Vec3 originalMotionMultiplier;
    private Vec3 motionMultiplier;

    public EntityStuckInBlockEvent(Entity entity, BlockState blockState, Vec3 motionMultiplier) {
        super(entity);
        this.blockState = blockState;
        this.originalMotionMultiplier = motionMultiplier;
        this.motionMultiplier = motionMultiplier;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public Vec3 getOriginalMotionMultiplier() {
        return originalMotionMultiplier;
    }

    public Vec3 getMotionMultiplier() {
        return motionMultiplier;
    }

    public EntityStuckInBlockEvent setMotionMultiplier(Vec3 motionMultiplier) {
        this.motionMultiplier = motionMultiplier;
        return this;
    }

}
