package net.sodiumzh.nfu.object;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.sodiumzh.nfu.exception.WrongSideException;

import javax.annotation.Nullable;

/**
 * Wrapper of an object that can be only accessed on server. Creating a {@code ServerOnly} instance
 * on client is okay, and you can safely declare it as a field. If accessed on client, it will
 * throw an exception, or return {@code null} if {@code setNoThrow} is called.
 */
public class ServerOnly<T> {

    @Nullable
    private T obj;
    @Nullable
    private Level levelContext;
    private boolean throwsOnWrongSide = true;

    public ServerOnly(@Nullable T obj, @Nullable Level levelContext) {
        this.obj = obj;
        this.levelContext = levelContext;
    }

    public ServerOnly(@Nullable T obj, Entity levelContext) {
        this(obj, levelContext == null ? null : levelContext.level());
    }

    public ServerOnly(@Nullable T obj) {
        this(obj, (Level) null);
    }

    /**
     * Set this instance should not throw exception but simply return null when accessed on the wrong side.
     */
    public ServerOnly<T> setNoThrow() {
        throwsOnWrongSide = false;
        return this;
    }

    public T get() {
        if ((levelContext != null && levelContext.isClientSide) || (levelContext == null && EffectiveSide.get().isClient())) {
            if (throwsOnWrongSide)
                throw new WrongSideException("NFU#ServerOnly: Accessed on client.");
            else return null;
        }
        return obj;
    }

    public void set(@Nullable T obj) {
        if ((levelContext != null && levelContext.isClientSide) || (levelContext == null && EffectiveSide.get().isClient())) {
            if (throwsOnWrongSide)
                throw new WrongSideException("NFU#ServerOnly: Accessed on client.");
        }
        this.obj = obj;
    }
}
