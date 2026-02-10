package net.sodiumzh.nfu.entity.anger;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.sodiumzh.nfu.annotation.CapabilityImplementation;

@Deprecated(forRemoval = true, since = "0.x.32")
@CapabilityImplementation(caps = CConditionalNeutralMob.class)
public class ConditionalNeutralMob extends MobAngerHandler implements CConditionalNeutralMob {

    public ConditionalNeutralMob(Mob mob, MobAngerRules rules) {
        super(mob, rules);
    }

    @Override
    public boolean isNeutralTo(LivingEntity entity) {
        return false;
    }




}
