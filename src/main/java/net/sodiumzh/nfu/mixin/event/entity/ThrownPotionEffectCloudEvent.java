package net.sodiumzh.nfu.mixin.event.entity;

import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.Cancelable;
import net.sodiumzh.nfu.event.NFUEntityEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Posted when a {@link ThrownPotion} is generating an {@link AreaEffectCloud}.
 * You can call {@code getCloud} to access the cloud instance or
 * {@code setCloud} to reconstruct another cloud instance instead.
 */
public class ThrownPotionEffectCloudEvent {

    /**
     * Posted after the cloud instance is created and before setting parameters.
     */
    @Cancelable
    public static class Construct extends NFUEntityEvent<ThrownPotion> {
        @Nullable
        private AreaEffectCloud cloud;
        private ItemStack effectItemStack;
        private Potion effectPotion;

        public Construct(ThrownPotion entity, @Nullable AreaEffectCloud cloud, ItemStack effectItemStack, Potion effectPotion) {
            super(entity);
            this.cloud = cloud;
            this.effectItemStack = effectItemStack;
            this.effectPotion = effectPotion;
        }

        @Nullable
        public AreaEffectCloud getCloud() {
            return cloud;
        }

        public void setCloud(@Nullable AreaEffectCloud cloud) {
            this.cloud = cloud;
        }
    }

    /**
     * Posted before spawning the cloud instance to level.
     */
    @Cancelable
    public static class Spawn extends NFUEntityEvent<ThrownPotion> {
        @Nullable
        private AreaEffectCloud cloud;

        public Spawn(ThrownPotion entity, @Nonnull AreaEffectCloud cloud) {
            super(entity);
            this.cloud = cloud;
        }

        @Nullable
        public AreaEffectCloud getCloud() {
            return cloud;
        }

        public void setCloud(@Nullable AreaEffectCloud cloud) {
            this.cloud = cloud;
        }
    }

}
