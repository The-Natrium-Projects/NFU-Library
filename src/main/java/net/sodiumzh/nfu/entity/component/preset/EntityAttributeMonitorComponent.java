package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.entity.component.EntityComponentBase;
import net.sodiumzh.nfu.event.NFULivingEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * A component that monitors the owner living entity's attributes and notifies on change.
 */
public abstract class EntityAttributeMonitorComponent extends EntityComponentBase<LivingEntity> {

    protected HashMap<Attribute, Double> listened = new HashMap<Attribute, Double>();

    public EntityAttributeMonitorComponent(LivingEntity entity) {
        super(entity);
        this.setup();
        MinecraftForge.EVENT_BUS.post(new EntityAttributeMonitorComponent.SetupEvent(this));
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
    @SuppressWarnings("deprecation")
    public EntityAttributeMonitorComponent addListened(Attribute attribute)
    {
        // Use NaN to label an attribute position before entity attributes creation
        double val = getEntity().getAttributeValue(attribute);
        this.listened.put(attribute, val);
        return this;
    }

    // Update and detect change on tick
    @Override
    public void tick()
    {
        for (Attribute attr: getAllListened().keySet())
        {
            double oldVal = getAllListened().get(attr);
            double newVal;
            if (attr == null)
                newVal = Double.NaN;
            else newVal	= getEntity().getAttributeValue(attr);
            // NaN indicates the value is not available yet, so don't post event but still update value
            // After attribute is created the value will update to non-NaN
            if (!Double.isNaN(oldVal)
                && !Double.isNaN(newVal)
                && (oldVal - newVal > 0.0000001 || oldVal - newVal < -0.0000001))
            {
                this.onAttributeChange(attr, oldVal, newVal);
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

    public static class SetupEvent extends NFULivingEvent<LivingEntity>
    {
        private final EntityAttributeMonitorComponent component;

        public SetupEvent(EntityAttributeMonitorComponent component)
        {
            super(component.getEntity());
            this.component = component;
        }

        public void addListened(Attribute attr)
        {
            component.addListened(attr);
        }

        public EntityAttributeMonitorComponent getComponent() {
            return component;
        }
    }

    public static class ChangeEvent extends NFULivingEvent<LivingEntity> {

        private final EntityAttributeMonitorComponent component;
        private final Attribute attribute;
        private final double oldValue;
        private final double newValue;

        public ChangeEvent(EntityAttributeMonitorComponent component, Attribute attribute,
                           double oldValue, double newValue)
        {
            super(component.getEntity());
            this.component = component;
            this.attribute = attribute;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public EntityAttributeMonitorComponent getComponent() {
            return component;
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
    }
}
