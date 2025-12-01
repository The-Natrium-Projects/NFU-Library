package net.sodiumzh.nfu.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.effect.EmptyEffect;

public class NFUEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, NFULibrary.MOD_ID);

    // Registry body
    public static final RegistryObject<MobEffect> EMPTY = EFFECTS.register("empty_effect", EmptyEffect::new);

}
