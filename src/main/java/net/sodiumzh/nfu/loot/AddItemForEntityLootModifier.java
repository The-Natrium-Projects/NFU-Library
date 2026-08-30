package net.sodiumzh.nfu.loot;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.common.loot.LootModifierManager;
import net.minecraftforge.registries.ForgeRegistries;
import net.sodiumzh.nfu.annotation.Credit;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;


/**
 * <p>
 * <strong>Origin & License:</strong><br>
 * This code is derived from/copied from the project <strong>Hostile Mobs and Girls</strong>,
 * originally authored by <strong>Mechalopa</strong>.
 * </p>
 *
 * <p>
 * This component is licensed under the <strong>GNU Lesser General Public License v3.0</strong> (LGPL-3.0).
 * You may obtain a copy of the License at:
 * <a href="https://www.gnu.org/licenses/lgpl-3.0.html">https://www.gnu.org/licenses/lgpl-3.0.html</a>
 * or see the {@code LICENSE} file in the project root directory.
 * </p>
 *
 * @author Mechalopa (Original)
 * @see <a href="https://github.com/Mechalopa/Hostile-Mobs-and-Girls/blob/1.18.2/src/main/java/com/github/mechalopa/hmag/world/level/storage/loot/modifiers/AddItemForEntityLootModifier.java">...</a>
 */
@Credit("Hostile Mobs and Girls")
public class AddItemForEntityLootModifier extends LootModifier {
    private static final Gson GSON = Deserializers.createFunctionSerializer().create();
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
    public List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context)
    {
        if (context.getParamOrNull(LootContextParams.THIS_ENTITY) != null && context.getParamOrNull(LootContextParams.BLOCK_STATE) == null && this.addition != null)
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

    public static class Serializer extends GlobalLootModifierSerializer<AddItemForEntityLootModifier>
    {
        @Override
        public AddItemForEntityLootModifier read(ResourceLocation name, JsonObject object, LootItemCondition[] conditions)
        {
            LootItemFunction[] functions = object.has("functions") ? GSON.fromJson(object.get("functions"), LootItemFunction[].class) : new LootItemFunction[0];
            Item addition = ForgeRegistries.ITEMS.getValue(new ResourceLocation((GsonHelper.getAsString(object, "addition"))));
            return new AddItemForEntityLootModifier(conditions, functions, addition);
        }

        @Override
        public JsonObject write(AddItemForEntityLootModifier instance)
        {
            JsonObject json = makeConditions(instance.conditions);

            if (!ArrayUtils.isEmpty(instance.functions))
            {
                json.add("functions", GSON.toJsonTree(instance.functions));
            }

            json.addProperty("addition", ForgeRegistries.ITEMS.getKey(instance.addition).toString());
            return json;
        }
    }
}
