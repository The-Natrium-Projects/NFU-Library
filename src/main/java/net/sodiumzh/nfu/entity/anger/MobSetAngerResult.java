package net.sodiumzh.nfu.entity.anger;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * A result for setting anger actions.
 * @param target The uuid of the target.
 * @param isHandled True if it really set an anger and made some impact. False if nothing happened.
 * @param reason The reason of this anger-setting action. Empty if it's not with a reason.
 */
public record MobSetAngerResult(UUID target, boolean isHandled, Optional<MobAngerReason> reason) {

    public static MobSetAngerResult handled(LivingEntity target, @Nullable MobAngerReason reason) {
        return new MobSetAngerResult(target.getUUID(), true, Optional.ofNullable(reason));
    }

    public static MobSetAngerResult unhandled(LivingEntity target, @Nullable MobAngerReason reason) {
        return new MobSetAngerResult(target.getUUID(), false, Optional.ofNullable(reason));
    }

}
