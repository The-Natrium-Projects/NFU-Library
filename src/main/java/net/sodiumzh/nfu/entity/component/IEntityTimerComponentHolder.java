package net.sodiumzh.nfu.entity.component;

import javax.annotation.Nullable;
import java.util.UUID;

public interface IEntityTimerComponentHolder {

    EntityTimerComponent getTimer();

    void onTimerExpire(String key, boolean dying, @Nullable UUID specifiedUUID);

}
