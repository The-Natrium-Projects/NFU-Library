package net.sodiumzh.nfu.entity.vanillatrade;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * A wrapper of {@link IVanillaTradeListing} that can modify its selection weight without knowing the
 * implementation or affecting the original instance.
 * <p>Used in collection weight scaling in {@link VanillaTradeRegistry}.
 */
public class ScaledVanillaTradeListing implements IVanillaTradeListing {

    private final IVanillaTradeListing original;
    private double scale = 1;

    public ScaledVanillaTradeListing(@Nonnull IVanillaTradeListing original, double scale) {
        this.original = original;
        this.scale = scale;
    }

    public ScaledVanillaTradeListing(@Nonnull IVanillaTradeListing original) {
        this.original = original;
    }

    @Override
    public double getSelectionWeight() {
        return this.original.getSelectionWeight() * this.scale;
    }

    @Override
    public boolean isValid() {
        return this.original.isValid();
    }

    @Override
    public int getDefaultRequiredLevel() {
        return this.original.getDefaultRequiredLevel();
    }

    @Nonnull
    @Override
    public MerchantOffer getOffer(Entity pTrader, Random pRandom) {
        return original.getOffer(pTrader, pRandom);
    }

    @Override
    public String toString() {
        return "ScaledVanillaTradeListing {original = " + original + ", scale = " + this.scale + "}";
    }

    public IVanillaTradeListing getOriginal() {
        return original;
    }

    public double getScale() {
        return scale;
    }
}
