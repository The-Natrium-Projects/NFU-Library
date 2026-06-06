package net.sodiumzh.nfu.entity.component.preset;

import net.sodiumzh.nfu.entity.component.preset.EntityTimerComponent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A utility interface to handle {@link EntityTimerComponent} expiration events in entity class.
 */
public interface IEntityTimerComponentAccess {

    void onTimerExpire(EntityTimerComponent<?> component, String key, boolean terminating, @Nullable UUID specifiedUUID);

    void onDefaultTimerExpire(String key, boolean terminating, @Nullable UUID specifiedUUID);

}
