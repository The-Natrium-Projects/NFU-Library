package net.sodiumzh.nfu.block;

import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

/**
 * A user-friendly version of {@link Level#setBlock} flag arguments, which is encoded in an integer as the method argument.
 * <p>Use {@link SetBlockFlag#codeOf(SetBlockFlag...)} to encode flags into the integer for method input.
 * <p>Use {@link SetBlockFlag#flagsOf(int)} to decode the integer to flag enums.
 * <p>Usage example:
 * <p>{@code level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), SetBlockFlag.codeOf(SetBlockFlag.CAUSES_BLOCK_UPDATE, SetBlockFlag.SYNC_TO_CLIENTS));}
 * <p>Note: after all this enum is slower than a single integer. For contexts of batch block replacement, cache the integer code first
 * instead of running {@code codeOf} for every call of {@code setBlock}.
 */
public enum SetBlockFlag {
    CAUSES_BLOCK_UPDATE(1),
    SYNC_TO_CLIENTS(2),
    NO_RERENDER(4),
    RERENDERS_ON_MAIN_THREAD(8),
    NO_NEIGHBOR_REACTIONS(16),
    NEIGHBOR_REACTION_NO_SPAWNING_DROPS(32),
    SIGNIFIES_MOVED(64);

    private final int byteMask;

    private SetBlockFlag(int byteMask) {
        this.byteMask = byteMask;
    }

    public int getByteMask(){
        return byteMask;
    }

    public static int codeOf(SetBlockFlag... flags) {
        return Arrays.stream(flags).mapToInt(SetBlockFlag::getByteMask).sum();
    }

    public static List<SetBlockFlag> flagsOf(int code) {
        return Arrays.stream(SetBlockFlag.values()).filter(flag -> (flag.getByteMask() & code) != 0).toList();
    }

}
