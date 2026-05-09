package net.sodiumzh.nfu.component;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.mixin.event.entity.EntityStartTickEvent;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A wrapper of an object which can tick and have a level reference. Can be either Entity, BlockEntity or Level.
 */
public class TickableLevelContext {

    private final Object obj;
    private final TickableLevelContext.Type type;

    private TickableLevelContext(Object obj) {
        this.obj = obj;
        if (obj instanceof Entity)
            type = Type.ENTITY;
        else if (obj instanceof Level)
            type = Type.LEVEL;
        else if (obj instanceof BlockEntity)
            type = Type.BLOCK_ENTITY;
        else throw new IllegalArgumentException("TickableLevelContext must be either Entity, Level or BlockEntity. Given: " + obj.getClass().getSimpleName());
    }

    public static TickableLevelContext ofEntity(Entity e) {
        return new TickableLevelContext(e);
    }

    public static TickableLevelContext ofLevel(Level l) {
        return new TickableLevelContext(l);
    }

    public static TickableLevelContext ofBlockEntity(BlockEntity be) {
        return new TickableLevelContext(be);
    }

    public Optional<Entity> asEntity() {
        return this.type.equals(Type.ENTITY) ? Optional.of((Entity)obj) : Optional.empty();
    }

    public Optional<Level> asLevel() {
        return this.type.equals(Type.LEVEL) ? Optional.of((Level)obj) : Optional.empty();
    }

    public Optional<BlockEntity> asBlockEntity() {
        return this.type.equals(Type.BLOCK_ENTITY) ? Optional.of((BlockEntity) obj) : Optional.empty();
    }

    /**
     * Get the level with this context.
     * <p>Always non-empty if it's an entity or a level. May be empty only when it's a block entity which doesn't have a level reference.
     */
    public Optional<Level> level() {
        return switch (this.type) {
            case ENTITY -> Optional.of(((Entity)obj).level());
            case LEVEL -> Optional.of((Level)obj);
            case BLOCK_ENTITY -> Optional.ofNullable(((BlockEntity)obj).getLevel());
        };
    }

    public enum Type {
        LEVEL,
        ENTITY,
        BLOCK_ENTITY
    }

}
