package net.sodiumzh.nfu.network;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.sodiumzh.nfu.exception.WrongSideException;

import java.util.Optional;

/**
 * Represents whether an object is available on server and on client.
 */
public enum AvailableSide {

    /**
     * Available on both sides. This means either it's a single instance accessible on all sides, or eich side keeps a valid instance.
     */
    BOTH("BOTH", true, true),
    /**
     * Available only on server. Access on client is meaningless and unsafe.
     */
    SERVER("SERVER", false, true),
    /**
     * Available only on client. Access on server is meaningless and unsafe.
     */
    CLIENT("CLIENT", true, false);

    private final String name;
    private final boolean isAvailableOnClient;
    private final boolean isAvailableOnServer;

    private AvailableSide(String name, boolean availableOnClient, boolean availableOnServer) {
        this.name = name;
        this.isAvailableOnClient = availableOnClient;
        this.isAvailableOnServer = availableOnServer;
    }

    public boolean isAvailableOnClient() {
        return isAvailableOnClient;
    }

    public boolean isAvailableOnServer() {
        return isAvailableOnServer;
    }

    /**
     * Check if the given side is correct for this side rule.
     */
    public boolean isCorrectSide(LogicalSide side) {
        return side.isServer() ? this.isAvailableOnServer : this.isAvailableOnClient;
    }

    /**
     * Check if on the correct side based on {@link EffectiveSide#get()} result.
     * <p>This is the last solution if no any context is present. Use versions with contexts if possible.
     */
    public boolean isCorrectSide() {
        return isCorrectSide(EffectiveSide.get());
    }

    public boolean isCorrectSide(Level ctx) {
        if (ctx == null) return isCorrectSide();
        return ctx.isClientSide() ? this.isAvailableOnClient : isAvailableOnServer;
    }

    public boolean isCorrectSide(Entity ctx) {
        return isCorrectSide(ctx.level());
    }

    public boolean isCorrectSide(BlockEntity ctx) {
        return ctx.getLevel() != null ? isCorrectSide(ctx.getLevel()) : isCorrectSide();
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Create a descriptive string for wrong side problem. For simplifying error message construction.
     */
    public static String describeWrongSide(LogicalSide currentSide) {
        if (currentSide.isServer()) {
            return "Component is for CLIENT only, but accessed on SERVER.";
        } else {
            return "Component is for SERVER only, but accessed on CLIENT.";
        }
    }

    /**
     * Create a descriptive string for wrong side problem. For simplifying error message construction.
     */
    public static String describeWrongSide(boolean isClientSide) {
        return describeWrongSide(isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER);
    }

    /**
     * Check if the side is correct, and throw if not.
     */
    public void assertCorrectSide(LogicalSide side) {
        if (!this.isCorrectSide(side))
            throw new WrongSideException(describeWrongSide(side));
    }

    /**
     * Check if the side is correct, and throw if not. (Using EffectiveSide result)
     */
    public void assertCorrectSide() {
        assertCorrectSide(EffectiveSide.get());
    }

    /**
     * Check if the side is correct, and throw if not.
     */
    public void assertCorrectSide(Level ctx) {
        if (!this.isCorrectSide(ctx))
            throw new WrongSideException(describeWrongSide(ctx.isClientSide()));
    }

    /**
     * Check if the side is correct, and throw if not.
     */
    public void assertCorrectSide(Entity ctx) {
        assertCorrectSide(ctx.level());
    }

    /**
     * Check if the side is correct, and throw if not.
     */
    public void assertCorrectSide(BlockEntity ctx) {
        Optional.ofNullable(ctx.getLevel()).ifPresentOrElse(this::assertCorrectSide, this::assertCorrectSide);
    }
}
