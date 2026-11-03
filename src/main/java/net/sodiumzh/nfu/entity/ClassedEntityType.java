package net.sodiumzh.nfu.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class ClassedEntityType<T extends Entity> {
    private final EntityType<T> type;
    private final Class<T> clazz;

    public ClassedEntityType(EntityType<T> type, Class<T> clazz) {
        this.clazz = clazz;
        this.type = type;
    }

    public static <T extends Entity> ClassedEntityType<T> of(EntityType<T> type, Class<T> clazz) {
        return new ClassedEntityType<>(type, clazz);
    }

    public EntityType<T> getEntityType() {
        return type;
    }

    public Class<T> getEntityClass() {
        return clazz;
    }

    public boolean is(EntityType<?> type) {
        return this.type == type;
    }

    public boolean equals(Object other) {
        if (other instanceof EntityType<?>) return type == other;
        else if (other instanceof ClassedEntityType<?> cet) return type == cet.type;
        else return false;
    }

    @Override
    public int hashCode() {
        return this.type.hashCode();
    }
}
