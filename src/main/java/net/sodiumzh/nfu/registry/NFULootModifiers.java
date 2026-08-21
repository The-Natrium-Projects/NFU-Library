package net.sodiumzh.nfu.registry;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.loot.AddItemForEntityLootModifier;
import net.sodiumzh.nfu.loot.ReplaceItemModifier;

public class NFULootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTRY =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NFULibrary.MOD_ID);

    public static final RegistryObject<Codec<AddItemForEntityLootModifier>> ADD_ITEM_FOR_ENTITY = REGISTRY.register("add_item_for_entity", AddItemForEntityLootModifier.CODEC);
    public static final RegistryObject<Codec<ReplaceItemModifier>> REPLACE_ITEM = REGISTRY.register("replace_item", ReplaceItemModifier.CODEC);

}
