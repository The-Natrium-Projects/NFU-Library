package net.sodiumzh.nfu.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.entity.AttachedItemDisplayerEntity;
import net.sodiumzh.nfu.entity.NFUEffectZoneEntity;
import net.sodiumzh.nfu.entity.NFUItemProjectileEntity;

public class NFUEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE,
        NFULibrary.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<AttachedItemDisplayerEntity>> ATTACHED_ITEM_DISPLAYER =
        ENTITY_TYPES.register("attached_item_displayer", () -> EntityType.Builder
            .of(AttachedItemDisplayerEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .noSave()
            .noSummon()
            .updateInterval(1)
            .build(new ResourceLocation(NFULibrary.MOD_ID, "attached_item_displayer").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<NFUItemProjectileEntity>> DEFAULT_ITEM_PROJECTILE
        = ENTITY_TYPES.register("default_item_projectile", () -> EntityType.Builder
        .of(NFUItemProjectileEntity::new, MobCategory.MISC)
        .sized(0.25f, 0.25f)
        .noSave()
        .noSummon()
        .updateInterval(1)
        .build(new ResourceLocation(NFULibrary.MOD_ID, "default_item_projectile").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<NFUEffectZoneEntity>> DEFAULT_EFFECT_ZONE
        = ENTITY_TYPES.register("default_effect_zone", () -> EntityType.Builder
        .of(NFUEffectZoneEntity::new, MobCategory.MISC)
        .sized(1f, 1f)
        .noSave()
        .noSummon()
        .updateInterval(1)
        .build(new ResourceLocation(NFULibrary.MOD_ID, "default_effect_zone").toString()));
}
