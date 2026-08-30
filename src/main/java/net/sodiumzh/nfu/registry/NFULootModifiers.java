package net.sodiumzh.nfu.registry;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.loot.AddItemForEntityLootModifier;
import net.sodiumzh.nfu.loot.ReplaceItemModifier;

public class NFULootModifiers {
    public static final DeferredRegister<GlobalLootModifierSerializer<?>> REGISTRY =
        DeferredRegister.create(ForgeRegistries.Keys.LOOT_MODIFIER_SERIALIZERS, NFULibrary.MOD_ID);

    public static final RegistryObject<GlobalLootModifierSerializer<?>> ADD_ITEM_FOR_ENTITY = REGISTRY.register("add_item_for_entity", AddItemForEntityLootModifier.Serializer::new);
    public static final RegistryObject<GlobalLootModifierSerializer<?>> REPLACE_ITEM = REGISTRY.register("replace_item", ReplaceItemModifier.Serializer::new);

}
