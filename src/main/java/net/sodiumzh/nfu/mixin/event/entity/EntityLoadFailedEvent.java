package net.sodiumzh.nfu.mixin.event.entity;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.sodiumzh.nfu.event.NFUEntityEvent;
import net.sodiumzh.nfu.exception.InfiniteRecursionException;
import net.sodiumzh.nfu.util.NFUDebugStatics;

/**
 * Posted when a failure happened during entity loading, allowing to manually fix the issues.
 */
public class EntityLoadFailedEvent extends NFUEntityEvent<Entity> {

    private final Throwable cause;
    private boolean shouldIgnore = false;
    private final CompoundTag readingNBT;

    public EntityLoadFailedEvent(Entity entity, Throwable cause, CompoundTag nbt) {
        super(entity);
        this.cause = cause;
        this.readingNBT = nbt;
    }

    /**
     * Get the throwable that caused this failure.
     */
    public Throwable getCause() {
        return cause;
    }

    private static long ID = 0;

    /**
     * Ignore the failure and force the entity to be added to the level.
     * <p>Be very careful using this! Use this only when you know what you are ignoring, and
     * ensure the issue has been fixed manually.
     * <p>Generally, you'll want to call {@link Entity#load} again after fixing the issue,
     * as code after the error site has not been invoked and the entity is probably not
     * loaded completely.
     * @param shouldPrintStackTrace If true, the throwable will print stack trace to log.
     * @throws InfiniteRecursionException When it's calling {@link Entity#load} or {@link EntityLoadFailedEvent#reload}
     *  but without the exception fixed. This will cause an infinite recursion because this event is posted inside {@code Entity#load}.
     *  This exception bypasses entity loading exception catching and causes game crash.
     */
    public void ignore(boolean shouldPrintStackTrace) {
        if (ID % 100 == 0 && this.detectRecursiveLoading())
            throw new InfiniteRecursionException("NFU EntityLoadFailedEvent: Ignored and reloaded with exception unhandled. This will cause an infinite recursion. Ensure the exception is correctly handled before reloading.");
        this.shouldIgnore = true;
        NFUDebugStatics.errorOnce(EntityLoadFailedEvent.class, "NFU: Entity %s loading failed. Ignored.");
        if (shouldPrintStackTrace){
            cause.printStackTrace();
        }
    }

    /**
     * Try loading the entity again from NBT. You'll want to call {@link EntityLoadFailedEvent#ignore(boolean)} to catch the exception
     * before calling this.
     * @throws InfiniteRecursionException When it's calling {@link Entity#load} or {@link EntityLoadFailedEvent#reload}
     *  but without the exception fixed. This will cause an infinite recursion because this event is posted inside {@code Entity#load}.
     *  This exception bypasses entity loading exception catching and causes game crash.
     */
    public void reload() {
        if (ID % 100 == 0 && this.detectRecursiveLoading())
            throw new InfiniteRecursionException("NFU EntityLoadFailedEvent: Ignored and reloaded with exception unhandled. This will cause an infinite recursion. Ensure the exception is correctly handled before reloading.");
        this.getEntity().load(this.getReadingNBT());
    }

    private boolean detectRecursiveLoading() {
        return StackWalker.getInstance().walk(sfs ->
            sfs.filter(sf -> sf.getMethodName().equals(ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_20258_"))))
            .limit(2).toList().size() >= 2;

    }

    public boolean isShouldIgnore() {
        return shouldIgnore;
    }

    public CompoundTag getReadingNBT() {
        return readingNBT;
    }
}
