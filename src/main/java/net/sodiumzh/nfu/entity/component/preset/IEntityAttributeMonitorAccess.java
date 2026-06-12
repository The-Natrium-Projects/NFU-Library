package net.sodiumzh.nfu.entity.component.preset;

import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.ApiStatus;

/**
 * A utility interface to operate {@link EntityAttributeMonitorComponent} in the entity class definition.
 */
public interface IEntityAttributeMonitorAccess extends IEntityComponentAccess {

    /**
     * Action when a component detects an attribute change.
     * <p>Note: If an attribute is monitored by multiple monitor components, this method will be invoked multiple times, once for each component.
     * Filter the component source before actions.</p>
     */
    @ApiStatus.OverrideOnly
    void onAttributeChange(EntityAttributeMonitorComponent source, Attribute attribute, double oldValue, double newValue);

    /**
     * Setup attribute monitor components, and add listened attributes.
     */
    @ApiStatus.OverrideOnly
    void setupAttributeMonitor(EntityAttributeMonitorComponent component);
}
