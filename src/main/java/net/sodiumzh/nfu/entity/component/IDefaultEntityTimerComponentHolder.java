package net.sodiumzh.nfu.entity.component;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A utility interface to handle {@link DefaultEntityTimerComponent} expiration events entity class.
 */
public interface IDefaultEntityTimerComponentHolder {

    EntityTimerComponent<Entity> getTimer();

    void onTimerExpire(String key, boolean dying, @Nullable UUID specifiedUUID);

}
