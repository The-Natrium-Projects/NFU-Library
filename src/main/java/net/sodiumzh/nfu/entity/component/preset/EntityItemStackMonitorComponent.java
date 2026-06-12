package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.sodiumzh.nfu.entity.component.EntityComponentBase;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A component that monitors item stacks and notifies on change.
 */
public abstract class EntityItemStackMonitorComponent extends EntityComponentBase<Entity> {

    protected HashMap<String, Supplier<ItemStack>> listened = new HashMap<>();
    protected HashMap<String, ItemStack> stacksLastTick = new HashMap<String, ItemStack>();

    public EntityItemStackMonitorComponent(Entity entity) {
        super(entity);
        this.setup();
        if (entity instanceof IEntityItemStackMonitorAccess access)
            access.setupItemStackMonitor(this);
    }

    public HashMap<String, Supplier<ItemStack>> getListenedStacks() {
        return listened;
    }

    public void addListened(String key, Supplier<ItemStack> getter) {
        listened.put(key, getter);
        ItemStack current = Optional.ofNullable(getter.get()).map(ItemStack::copy).orElse(ItemStack.EMPTY);
        stacksLastTick.put(key, current);
    }

    @Override
    public void tick() {
        for (String key: listened.keySet())
        {
            ItemStack newStack = Optional.ofNullable(listened.get(key)).map(Supplier::get).orElse(ItemStack.EMPTY);
            if (!newStack.equals(stacksLastTick.get(key), false))
            {
                this.onChanged(key, stacksLastTick.get(key).copy(), newStack.copy());
                if (this.getEntity() instanceof IEntityItemStackMonitorAccess access)
                    access.onItemStackChange(this, key, stacksLastTick.get(key).copy(), newStack.copy());
                this.stacksLastTick.put(key, newStack);
            }
        }
    }

    @Override
    @Nullable
    public CompoundTag serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }

    public abstract void setup();

    public abstract void onChanged(String key, ItemStack oldItemStackCopy, ItemStack newItemStackCopy);

    public static class Default extends EntityItemStackMonitorComponent {

        public Default(Entity entity) {
            super(entity);
        }

        @Override
        public void setup() {

        }

        @Override
        public void onChanged(String key, ItemStack oldItemStackCopy, ItemStack newItemStackCopy) {

        }
    }
}
