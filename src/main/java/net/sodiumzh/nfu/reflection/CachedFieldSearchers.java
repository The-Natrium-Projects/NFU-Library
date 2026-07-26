package net.sodiumzh.nfu.reflection;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.sodiumzh.nfu.container.ITable2D;
import net.sodiumzh.nfu.container.Table2D;
import net.sodiumzh.nfu.exception.ReflectionFailedException;
import net.sodiumzh.nfu.object.CastableObject;
import net.sodiumzh.nfu.util.NFUReflectionStatics;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CachedFieldSearchers {

    private static final ThreadLocal<Map<Args, Optional<Field>>> CACHE = ThreadLocal.withInitial(HashMap::new);

    public static Optional<Field> findDeclaredField(Class<?> clazz, String name) {
        String remappedName = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.FIELD, name);
        // First check if recorded
        Args args = new Args(clazz, remappedName);
        @Nullable Optional<Field> f = CACHE.get().getOrDefault(args, null);
        // Here: null = not searched yet; empty = searched, no such field; non-empty = field exists
        if (f != null) return f;
        // Reflectively search the field
        // f is non-null below, only present/empty
        f = Optional.empty();
        try {
            f = Optional.ofNullable(clazz.getDeclaredField(remappedName));
        } catch (NoSuchFieldException | NoSuchFieldError e) {
            f = Optional.empty();
        }
        CACHE.get().put(args, f);
        f.ifPresent(field -> field.setAccessible(true));
        return f;
    }

    public static Optional<Object> getFieldValue(Object obj, Class<?> declaredClass, String name) {
        return findDeclaredField(declaredClass, name).map(f -> {
            try { return f.get(obj); } catch (IllegalAccessException e) { throw new ReflectionFailedException(e); }
        });
    }

    public static <T> Optional<T> getFieldValue(Object obj, Class<?> declaredClass, String name, Class<T> valueClass) {
        return getFieldValue(obj, declaredClass, name).filter(v -> valueClass.isAssignableFrom(v.getClass()))
            .map(v -> (T)v);
    }

    public static void clearCache() {
        CACHE.get().clear();
    }

    private static record Args(Class<?> declaredClass, String name) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Args args && this.declaredClass.equals(args.declaredClass) && this.name.equals(args.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(declaredClass, name);
        }

    }
}
