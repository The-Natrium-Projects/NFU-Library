package net.sodiumzh.nfu.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.effect.EmptyEffect;

public class NFUEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, NFULibrary.MOD_ID);

    // Registry body
    public static final DeferredHolder<MobEffect, MobEffect> EMPTY = EFFECTS.register("empty_effect", EmptyEffect::new);

}
