package net.sodiumzh.nfu.loot;

import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.sodiumzh.nfu.annotation.Credit;

@Credit("Hostile Mobs and Girls")
public class AddTableLootModifier extends LootModifier
{
    public static final Supplier<MapCodec<AddTableLootModifier>> CODEC =
        Suppliers.memoize(() -> RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
                .and(ResourceLocation.CODEC.fieldOf("table").forGetter(m -> m.lootTable)).apply(inst, AddTableLootModifier::new)));
    private final ResourceLocation lootTable;

    public AddTableLootModifier(LootItemCondition[] conditions, ResourceLocation lootTable)
    {
        super(conditions);
        this.lootTable = lootTable;
    }

    @SuppressWarnings("deprecation")
    @Nonnull
    @Override
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        LootTable table = context.getResolver().getLootTable(this.lootTable);
        table.getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), generatedLoot::add));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec()
    {
        return CODEC.get();
    }
}
