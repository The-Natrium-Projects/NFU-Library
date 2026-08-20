package net.sodiumzh.nfu.entity.anger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.eventbus.api.Cancelable;
import net.sodiumzh.nfu.event.NFULivingEvent;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Posted when a mob gets angry at a target. Posted through {@link MobAngerHandlerComponent}.
 */
@Cancelable
public class MobAngryAtEvent extends NFULivingEvent<Mob> {

    private final LivingEntity originalTarget;
    private LivingEntity target;
    @Nullable
    private final MobAngerReason originalReason;
    @Nullable
    private MobAngerReason reason;
    private final int originalForgivingTime;
    private int forgivingTime;

    public MobAngryAtEvent(Mob entity, LivingEntity target, MobAngerReason reason, int forgivingTime) {
        super(entity);
        this.originalTarget = target;
        this.target = target;
        this.originalReason = reason;
        this.reason = reason;
        this.originalForgivingTime = forgivingTime;
        this.forgivingTime = forgivingTime;
    }

    public LivingEntity getOriginalTarget() {
        return originalTarget;
    }

    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Nullable. If null, it means this setting-anger action bypasses the anger reason mechanic,
     * and setting reason will not do anything.
     */
    @Nullable
    public MobAngerReason getOriginalReason() {
        return originalReason;
    }

    /**
     * Nullable. If null, it means this setting-anger action bypasses the anger reason mechanic,
     * and setting reason will not do anything.
     */
    @Nullable
    public MobAngerReason getReason() {
        return reason;
    }

    public int getOriginalForgivingTime() {
        return originalForgivingTime;
    }

    public int getForgivingTime() {
        return forgivingTime;
    }

    public MobAngryAtEvent setTarget(LivingEntity target) {
        this.target = target;
        return this;
    }

    /**
     * Set the reason (non-null).
     * Note: original reason is nullable. If null, it means this setting-anger action bypasses the anger reason mechanic,
     * and setting reason will not do anything.
     */
    public MobAngryAtEvent setReason(@Nonnull MobAngerReason reason) {
        Objects.requireNonNull(reason);
        this.reason = reason;
        return this;
    }

    public MobAngryAtEvent setForgivingTime(int forgivingTime) {
        this.forgivingTime = forgivingTime;
        return this;
    }
}
