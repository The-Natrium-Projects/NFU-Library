package net.sodiumzh.nfu.registry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.entity.anger.MobAngerHandlerComponent;
import net.sodiumzh.nfu.entity.anger.MobAngerRules;
import net.sodiumzh.nfu.entity.component.EntityComponentSetupEvent;
import net.sodiumzh.nfu.entity.component.EntityComponentType;
import net.sodiumzh.nfu.entity.component.SubComponentAccessor;
import net.sodiumzh.nfu.entity.component.preset.EntityAttributeMonitorComponent;
import net.sodiumzh.nfu.entity.component.preset.EntityItemStackMonitorComponent;
import net.sodiumzh.nfu.entity.component.preset.HealingHandlerComponent;
import net.sodiumzh.nfu.network.AvailableSide;
import net.sodiumzh.nfu.object.HierarchyPath;

public class NFUEntityComponents {

    public static final NFURegistryEntryCollection<EntityComponentType<?, ?>> COLLECTION =
        NFURegistryEntryCollection.create(NFURegistries.ENTITY_COMPONENT_TYPES, NFULibrary.MOD_ID);

    public static final NFURegistry.Accessor<EntityComponentType<Mob, MobAngerHandlerComponent>>
        DEFAULT_ANGER_HANDLER = COLLECTION.register("default_anger_handler", () ->
        new EntityComponentType<>(Mob.class, MobAngerHandlerComponent.class, AvailableSide.SERVER,
            mob -> new MobAngerHandlerComponent(mob, MobAngerRules.ATTACKER.get())));

    public static final HierarchyPath PATH_DEFAULT_ANGER_HANDLER = HierarchyPath.byLiteral("default_anger_handler");

    public static final SubComponentAccessor<Mob, MobAngerHandlerComponent> ACCESSOR_DEFAULT_ANGER_HANDLER
        = new SubComponentAccessor<>(PATH_DEFAULT_ANGER_HANDLER, DEFAULT_ANGER_HANDLER);

}
