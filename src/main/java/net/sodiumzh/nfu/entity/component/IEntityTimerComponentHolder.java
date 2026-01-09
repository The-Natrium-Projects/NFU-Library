package net.sodiumzh.nfu.entity.component;

public interface IEntityTimerComponentHolder {

    EntityTimerComponent getTimer();

    void onTimerExpire(String key, boolean dying);

}
