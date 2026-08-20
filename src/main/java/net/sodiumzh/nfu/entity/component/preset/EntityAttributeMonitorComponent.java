package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.common.MinecraftForge;
import net.sodiumzh.nfu.entity.component.EntityComponentBase;
import net.sodiumzh.nfu.entity.component.EntityComponentEvent;
import net.sodiumzh.nfu.event.NFULivingEvent;
import net.sodiumzh.nfu.network.AvailableSide;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A component that monitors the owner living entity's attributes and notifies on change.
 */
public abstract class EntityAttributeMonitorComponent extends EntityComponentBase<LivingEntity> {

    protected HashMap<Attribute, Double> listened = new HashMap<Attribute, Double>();

    public EntityAttributeMonitorComponent(LivingEntity entity) {
        super(entity);
        this.setup();
        if (entity instanceof IEntityAttributeMonitorAccess access)
            access.setupAttributeMonitor(this);
    }

    /**
     * Get the listened attribute list
     * Key: attribute
     * Value: current attribute value
     */
    public Map<Attribute, Double> getAllListened() {
        return this.listened;
    }

    /** Add an attribute to the listen list.
     * It still works if the attribute isn't available yet (e.g. on capability attachment).
     */
    public EntityAttributeMonitorComponent addListened(Attribute attribute)
    {
        // Use NaN to label an attribute position before entity attributes creation
        double val = Optional.ofNullable(this.getEntity().getAttributes())
            .map(am -> am.getValue(attribute))
            .orElse(Double.NaN);
        this.listened.put(attribute, val);
        return this;
    }

    // Update and detect change on tick
    @Override
    public void tick()
    {
        for (Attribute attr: getAllListened().keySet().stream().toList())
        {
            double oldVal = getAllListened().get(attr);
            double newVal;
            if (attr == null)
                newVal = Double.NaN;
            else newVal	= getEntity().getAttributeValue(attr);
            // NaN indicates the value is not available yet, so don't post event but still update value
            // After attribute is created the value will update to non-NaN
            if ((Double.isNaN(oldVal) ^ Double.isNaN(newVal))
                || (!Double.isNaN(oldVal) && !Double.isNaN(newVal) & Math.abs(oldVal - newVal) > 1e-12d))
            {
                this.onAttributeChange(attr, oldVal, newVal);
                if (this.getEntity() instanceof IEntityAttributeMonitorAccess access)
                    access.onAttributeChange(this, attr, oldVal, newVal);
                MinecraftForge.EVENT_BUS.post(new EntityAttributeMonitorComponent.ChangeEvent(this, attr, oldVal, newVal));
            }
            getAllListened().put(attr, newVal);
        }
    }

    /**
     * Auto-invoked in constructor to add listened components.
     */
    @ApiStatus.OverrideOnly
    public abstract void setup();

    /**
     * Auto-invoked on attribute changes.
     */
    @ApiStatus.OverrideOnly
    public abstract void onAttributeChange(Attribute attribute, double oldValue, double newValue);

    public static class ChangeEvent extends EntityComponentEvent<LivingEntity, EntityAttributeMonitorComponent> {

        private final Attribute attribute;
        private final double oldValue;
        private final double newValue;

        public ChangeEvent(EntityAttributeMonitorComponent component, Attribute attribute,
                           double oldValue, double newValue)
        {
            super(component.getEntity(), component);
            this.attribute = attribute;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public Attribute getAttribute() {
            return attribute;
        }

        public double getOldValue() {
            return oldValue;
        }

        public double getNewValue() {
            return newValue;
        }

        public boolean involvesNaN() {
            return Double.isNaN(oldValue) || Double.isNaN(newValue);
        }
    }

    public static class Default extends EntityAttributeMonitorComponent {

        public Default(LivingEntity entity) {
            super(entity);
        }

        @Nullable
        @Override
        public CompoundTag serializeNBT() {
            return null;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
        }

        @Override
        public void setup() {
        }

        @Override
        public void onAttributeChange(Attribute attribute, double oldValue, double newValue) {
        }
    }
}
