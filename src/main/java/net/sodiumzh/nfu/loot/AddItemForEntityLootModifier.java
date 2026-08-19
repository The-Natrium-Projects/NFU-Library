package net.sodiumzh.nfu.loot;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.sodiumzh.nfu.annotation.Credit;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

@Credit("Hostile Mobs and Girls")
public class AddItemForEntityLootModifier extends LootModifier {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final Codec<LootItemFunction[]> LOOT_FUNCTIONS_CODEC = Codec.PASSTHROUGH.flatXmap(d -> {
        try
        {
            LootItemFunction[] functions = GSON.fromJson(d.convert(JsonOps.INSTANCE).getValue(), LootItemFunction[].class);
            return DataResult.success(functions);
        }
        catch (JsonSyntaxException e)
        {
            LogUtils.getLogger().warn("Unable to decode loot functions", e);
            return DataResult.error(e::getMessage);
        }
    }, functions -> {
        try
        {
            JsonElement element = GSON.toJsonTree(functions);
            return DataResult.success(new Dynamic<>(JsonOps.INSTANCE, element));
        }
        catch (JsonSyntaxException e)
        {
            LogUtils.getLogger().warn("Unable to encode loot functions", e);
            return DataResult.error(e::getMessage);
        }
    });

    public static final Supplier<MapCodec<AddItemForEntityLootModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.mapCodec(inst -> codecStart(inst).and(inst.group(LOOT_FUNCTIONS_CODEC.optionalFieldOf("functions", new LootItemFunction[0]).forGetter(m -> m.functions),BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("addition", Items.BARRIER).forGetter(m -> m.addition))).apply(inst, AddItemForEntityLootModifier::new)));
    private final LootItemFunction[] functions;
    private final Item addition;

    public AddItemForEntityLootModifier(LootItemCondition[] conditions, LootItemFunction[] functions, Item addition)
    {
        super(conditions);
        this.functions = functions;
        this.addition = addition;
    }

    @Nonnull
    @Override
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        if (context.getParamOrNull(LootContextParams.THIS_ENTITY) != null && context.getParamOrNull(LootContextParams.BLOCK_STATE) == null && this.addition != null && !this.addition.equals(Items.BARRIER))
        {
            ItemStack stack = this.addition.getDefaultInstance();

            for (LootItemFunction function : this.functions)
            {
                stack = function.apply(stack, context);
            }

            generatedLoot.add(stack);
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec()
    {
        return CODEC.get();
    }
}
