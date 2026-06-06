package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Cancelable;
import net.sodiumzh.nfu.entity.component.EntityComponentBase;
import net.sodiumzh.nfu.event.NFULivingEvent;
import net.sodiumzh.nfu.util.NFUItemStatics;
import net.sodiumzh.nfu.util.NFUParticleStatics;

import javax.annotation.Nullable;

/**
 * An entity component that handles healing-by-item game mechanics with a cooldown time.
 * <p>This component itself only does healing and item consumption, and doesn't handle other mechanics (e.g. interaction).
 * <p>To add other actions on applying healing, listen to the related events.
 */
public class HealingHandlerComponent extends EntityComponentBase<LivingEntity> {

    private int cooldown = 0;

    public HealingHandlerComponent(LivingEntity entity) {
        super(entity);
    }

    @Override
    public void tick() {
        if (this.cooldown > 0) --this.cooldown;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("cooldown", this.cooldown);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.cooldown = nbt.getInt("cooldown");
    }

    @Deprecated
    public int getHealingCooldownTicks()
    {
        return 40;
    }

    public int getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(int value) {
        this.cooldown = value;
    }

    public boolean applyHealingItem(ItemStack stack, float value, boolean consume, int cooldown, @Nullable Player player)
    {
        if (this.getEntity().getHealth() < this.getEntity().getMaxHealth() && getCooldown() == 0)
        {
            HealingHandlerComponent.ApplyHealingItemEvent event = new HealingHandlerComponent.ApplyHealingItemEvent(this, stack, value, cooldown);
            ItemStack cpy = stack.copy();
            float oldHP = this.getEntity().getHealth();
            boolean canceled = MinecraftForge.EVENT_BUS.post(event);
            if (!canceled)
            {
                if (consume)
                {
                    ItemStack remaining = stack.getCraftingRemainingItem();
                    stack.shrink(1);
                    if (player != null)
                        NFUItemStatics.giveOrDrop(player, remaining);
                }
                this.getEntity().heal(event.getHealValue());
                if (event.shouldSendDefaultParticles())
                    sendParticlesOnSuccess();
                if (event.getCooldown() > 0)
                    setCooldown(event.getCooldown());
                MinecraftForge.EVENT_BUS.post(new HealingHandlerComponent.HealingSucceededEvent(this, cpy, this.getEntity().getHealth() - oldHP, player));
                return true;
            }
        }
        HealingHandlerComponent.HealingFailedEvent event = new HealingHandlerComponent.HealingFailedEvent(this, stack, player);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.shouldSendDefaultParticles())
            sendParticlesOnFailure();
        return false;
    }

    public void sendParticlesOnSuccess()
    {
        NFUParticleStatics.sendGlintParticlesToEntityDefault(this.getEntity());
    }

    public void sendParticlesOnFailure()
    {
        NFUParticleStatics.sendSmokeParticlesToEntityDefault(this.getEntity());
    }

    @Cancelable
    public static class ApplyHealingItemEvent extends NFULivingEvent<LivingEntity>
    {
        private final HealingHandlerComponent component;
        /** Item stack to apply. */
        private final ItemStack stack;
        /** HP value to heal. Settable. */
        private float healValue;
        /** Whether it should send default particles (glint). Settable. */
        private boolean sendDefaultParticles = true;
        /** Cooldown ticks. Settable. */
        private int cooldown;

        public ApplyHealingItemEvent(HealingHandlerComponent component, ItemStack stack, float healValue, int cooldown)
        {
            super(component.getEntity());
            this.component = component;
            this.stack = stack;
            this.healValue = healValue;
            this.cooldown = cooldown;
        }

        public HealingHandlerComponent getComponent() {
            return component;
        }

        public ItemStack getStack() {
            return stack;
        }

        public float getHealValue() {
            return healValue;
        }

        public void setHealValue(float healValue) {
            this.healValue = healValue;
        }

        public boolean shouldSendDefaultParticles() {
            return sendDefaultParticles;
        }

        public void setSendDefaultParticles(boolean sendDefaultParticles) {
            this.sendDefaultParticles = sendDefaultParticles;
        }

        public int getCooldown() {
            return cooldown;
        }

        public void setCooldown(int cooldown) {
            this.cooldown = cooldown;
        }
    }

    /**
     * Fired when applying healing item to living succeeded.
     */
    public static class HealingSucceededEvent extends NFULivingEvent<LivingEntity>
    {
        private final HealingHandlerComponent component;
        /** Item stack applied. It's a copy of the item stack before applying. */
        private final ItemStack stack;
        /** HP value actually applied. */
        private final float healedValue;
        /** Player performing this healing action. **/
        @Nullable
        private final Player player;

        public HealingSucceededEvent(HealingHandlerComponent component, ItemStack stack, float healedValue, @Nullable Player player)
        {
            super(component.getEntity());
            this.component = component;
            this.stack = stack;
            this.healedValue = healedValue;
            this.player = player;
        }

        public HealingHandlerComponent getComponent() {
            return component;
        }

        public ItemStack getStack() {
            return stack;
        }

        public float getHealedValue() {
            return healedValue;
        }

        @Nullable
        public Player getPlayer() {
            return player;
        }
    }

    /**
     * Fired when applying healing item to living failed, including because of cancellation of {@link HealingHandlerComponent.ApplyHealingItemEvent}.
     */
    public static class HealingFailedEvent extends NFULivingEvent<LivingEntity>
    {
        private final HealingHandlerComponent component;
        /** Item stack to apply. */
        public final ItemStack stack;
        /** Whether it should send default particles (smoke). Settable. */
        public boolean sendDefaultParticles = true;
        /** Player performing this healing action. **/
        @Nullable
        public final Player player;

        public HealingFailedEvent(HealingHandlerComponent component, ItemStack stack, @Nullable Player player)
        {
            super(component.getEntity());
            this.component = component;
            this.stack = stack;
            this.player = player;
        }

        public HealingHandlerComponent getComponent() {
            return component;
        }

        public ItemStack getStack() {
            return stack;
        }

        public boolean shouldSendDefaultParticles() {
            return sendDefaultParticles;
        }

        public void setSendDefaultParticles(boolean sendDefaultParticles) {
            this.sendDefaultParticles = sendDefaultParticles;
        }

        @Nullable
        public Player getPlayer() {
            return player;
        }
    }
    
}
