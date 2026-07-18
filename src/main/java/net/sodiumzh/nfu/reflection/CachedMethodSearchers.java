package net.sodiumzh.nfu.reflection;

import net.sodiumzh.nfu.container.Tuple2;
import net.sodiumzh.nfu.exception.ReflectionFailedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CachedMethodSearchers {

    private static final ThreadLocal<Map<Args, Optional<Method>>> CACHE = ThreadLocal.withInitial(HashMap::new);

    public static Optional<Method> findDeclaredMethod(Class<?> declaredClass, String name, Class<?>... paramClasses) {
        Args args = new Args(declaredClass, name, paramClasses);
        Optional<Method> m = CACHE.get().getOrDefault(args, null);
        // If has a record, directly return
        if (m != null) return m;
        // Try finding method
        m = Optional.empty();
        try {
            m = Optional.ofNullable(declaredClass.getDeclaredMethod(name, paramClasses));
        } catch (NoSuchMethodException | NoSuchMethodError e) {
            m = Optional.empty();
        }
        m.ifPresent(me -> me.setAccessible(true));
        CACHE.get().put(args, m);
        return m;
    }

    public static <T> Optional<Object> invokeIfPresent(T caller, Class<? super T> declaredClass, String name, Tuple2<Class<?>, Object>[] paramClassesAndValues) {
        return findDeclaredMethod(declaredClass, name, Arrays.stream(paramClassesAndValues).map(Tuple2::getA).toArray(Class<?>[]::new))
            .map(m -> {
                try {
                    return m.invoke(caller, Arrays.stream(paramClassesAndValues).map(Tuple2::getB).toArray());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ReflectionFailedException(e);
                }
            });
    }

    public static <T, P1> Optional<Object> invokeIfPresent(T caller, Class<? super T> declaredClass, String name, Class<? super P1> p1Class, P1 p1Value) {
        return findDeclaredMethod(declaredClass, name, p1Class)
            .map(m -> {
                try {
                    return m.invoke(caller, p1Value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ReflectionFailedException(e);
                }
            });
    }

    public static <T, P1, P2> Optional<Object> invokeIfPresent(T caller, Class<? super T> declaredClass, String name,
                                                           Class<? super P1> p1Class, P1 p1Value, Class<? super P2> p2Class, P2 p2Value) {
        return findDeclaredMethod(declaredClass, name, p1Class, p2Class)
            .map(m -> {
                try {
                    return m.invoke(caller, p1Value, p2Value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ReflectionFailedException(e);
                }
            });
    }

    public static <T, P1, P2, P3> Optional<Object> invokeIfPresent(T caller, Class<? super T> declaredClass, String name,
                                                               Class<? super P1> p1Class, P1 p1Value, Class<? super P2> p2Class, P2 p2Value,
                                                               Class<? super P3> p3Class, P3 p3Value) {
        return findDeclaredMethod(declaredClass, name, p1Class, p2Class, p3Class)
            .map(m -> {
                try {
                    return m.invoke(caller, p1Value, p2Value, p3Value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ReflectionFailedException(e);
                }
            });
    }

    public static <T, P1, P2, P3, P4> Optional<Object> invokeIfPresent(T caller, Class<? super T> declaredClass, String name,
                                                                   Class<? super P1> p1Class, P1 p1Value, Class<? super P2> p2Class, P2 p2Value,
                                                                   Class<? super P3> p3Class, P3 p3Value, Class<? super P4> p4Class, P4 p4Value) {
        return findDeclaredMethod(declaredClass, name, p1Class, p2Class, p3Class, p4Class)
            .map(m -> {
                try {
                    return m.invoke(caller, p1Value, p2Value, p3Value, p4Value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ReflectionFailedException(e);
                }
            });
    }

    public static <T, P1, P2, P3, P4, P5> Optional<Object> invokeIfPresent(T caller, Class<? super T> declaredClass, String name,
                                                                       Class<? super P1> p1Class, P1 p1Value, Class<? super P2> p2Class, P2 p2Value,
                                                                       Class<? super P3> p3Class, P3 p3Value, Class<? super P4> p4Class, P4 p4Value,
                                                                       Class<? super P5> p5Class, P5 p5Value) {
        return findDeclaredMethod(declaredClass, name, p1Class, p2Class, p3Class, p4Class, p5Class)
            .map(m -> {
                try {
                    return m.invoke(caller, p1Value, p2Value, p3Value, p4Value, p5Value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ReflectionFailedException(e);
                }
            });
    }

    public static <T, P1, P2, P3, P4, P5, P6> Optional<Object> invokeIfPresent(T caller, Class<? super T> declaredClass, String name,
                                                                           Class<? super P1> p1Class, P1 p1Value, Class<? super P2> p2Class, P2 p2Value,
                                                                           Class<? super P3> p3Class, P3 p3Value, Class<? super P4> p4Class, P4 p4Value,
                                                                           Class<? super P5> p5Class, P5 p5Value, Class<? super P6> p6Class, P6 p6Value) {
        return findDeclaredMethod(declaredClass, name, p1Class, p2Class, p3Class, p4Class, p5Class, p6Class)
            .map(m -> {
                try {
                    return m.invoke(caller, p1Value, p2Value, p3Value, p4Value, p5Value, p6Value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ReflectionFailedException(e);
                }
            });
    }

    private static record Args(Class<?> declaredClass, String name, Class<?>[] paramClasses) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Args otherArgs
                && this.declaredClass.equals(otherArgs.declaredClass)
                && this.name.equals(otherArgs.name)
                && Arrays.equals(this.paramClasses, otherArgs.paramClasses);
        }

        @Override
        public int hashCode() {
            return Objects.hash(declaredClass, name, Arrays.hashCode(paramClasses));
        }
    }


}
