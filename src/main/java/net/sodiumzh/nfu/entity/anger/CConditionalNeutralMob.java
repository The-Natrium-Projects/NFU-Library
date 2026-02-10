package net.sodiumzh.nfu.entity.anger;

import net.minecraft.world.entity.LivingEntity;
import net.sodiumzh.nfu.annotation.CapabilityInterface;

@Deprecated(forRemoval = true, since = "0.x.32")
@CapabilityInterface
public interface CConditionalNeutralMob extends CMobAngerHandler {

    public boolean isNeutralTo(LivingEntity entity);

}
