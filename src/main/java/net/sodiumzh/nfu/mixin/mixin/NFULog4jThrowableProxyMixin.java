package net.sodiumzh.nfu.mixin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;

@Mixin(ThrowableProxy.class)
public class NFULog4jThrowableProxyMixin {

    @Unique
    private static long NFU$CHECK_COUNTER = 0L;

    @WrapOperation(method = "<init>(Ljava/lang/Throwable;Ljava/util/Deque;Ljava/util/Map;Ljava/lang/Throwable;Ljava/util/Set;Ljava/util/Set;)V",
        at = @At(value = "INVOKE",
            target = "java/lang/Throwable.getCause()Ljava/lang/Throwable;"),
        remap = false, require = -1, expect = -1)
    private Throwable nfu_checkInfiniteRecursion(
        Throwable instance,
        Operation<Throwable> original)
    {
        if (NFU$CHECK_COUNTER % 256 == 255) {
            boolean isInfinite = StackWalker.getInstance().walk(stream -> {
                List<StackWalker.StackFrame> snapshot =
                    stream.limit(128).toList();
                if (snapshot.isEmpty()) return false;
                if (snapshot.get(snapshot.size() - 1).getDeclaringClass().equals(ThrowableProxy.class))
                    return true;
                else return false;
            });
            if (isInfinite) {
                LogUtils.getLogger().error("Cause chain interrupted due to probable infinite recursion.");
                return null;
            }
        }
        ++NFU$CHECK_COUNTER;
        return original.call(instance);
    }


}
