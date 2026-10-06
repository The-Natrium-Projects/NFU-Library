package net.sodiumzh.nfu.entity.taming;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class TamingInteractionResult {

    private final Level level;
    private InteractionResult result = InteractionResult.PASS;
    @Nullable
    private Mob tamed = null;

    private TamingInteractionResult(Level level) { this.level = level; }

    public static TamingInteractionResult of(Level level, InteractionResult result, Mob tamed)
    {
        TamingInteractionResult res = new TamingInteractionResult(level);
        res.result = result;
        return res;
    }

    public static TamingInteractionResult of(Level level, InteractionResult result) {
        return of(level, result, null);
    }

    /**
     * Indicates that interaction is not handled in {@link ITamingProcess#handleInteract}, and should be passed to the
     * next step i.e. {@link Mob#mobInteract}.
     */
    public static TamingInteractionResult unhandled(Level level) {
        return of(level, InteractionResult.PASS, null);
    }

    /**
     * Indicates that interaction is not handled in {@link ITamingProcess#handleInteract}, and should be passed to the
     * next step i.e. {@link Mob#mobInteract}.
     */
    public static TamingInteractionResult unhandled(Entity context) {
        return of(context.level(), InteractionResult.PASS, null);
    }

    /**
     * Get the interaction result. {@link InteractionResult#PASS} means unhandled and the interaction should be passed to the next step.
     * {@link InteractionResult#sidedSuccess} means handled and the interaction should stop here.
     */
    public InteractionResult getResult() {
        return result;
    }

    public void setResult(InteractionResult result) {
        this.result = result;
    }

    @Nonnull
    public Optional<Mob> getTamedMob() {
        return Optional.ofNullable(tamed);
    }

    public void setTamedMob(@Nullable Mob tamed) {
        this.tamed = tamed;
    }

    public Level getLevel() {
        return level;
    }

}
