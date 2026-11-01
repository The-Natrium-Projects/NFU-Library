package net.sodiumzh.nfu.compat.jer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import jeresources.api.drop.LootDrop;
import jeresources.config.Settings;
import jeresources.entry.DungeonEntry;
import jeresources.entry.MobEntry;
import jeresources.platform.Services;
import jeresources.registry.DungeonRegistry;
import jeresources.registry.MobRegistry;
import jeresources.util.LootTableHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifierManager;
import net.minecraftforge.registries.ForgeRegistries;
import net.sodiumzh.nfu.loot.AddItemForEntityLootModifier;
import net.sodiumzh.nfu.loot.AddTableLootModifier;
import net.sodiumzh.nfu.loot.ReplaceItemModifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import static net.sodiumzh.nfu.compat.jer.JERLootConditions.Match;

final class JERLootCompat
{
    private static final Logger LOGGER = LogManager.getLogger();

    private JERLootCompat() {}

    static void update()
    {
        List<Definition> modifiers = loadModifiers();
        if (modifiers.isEmpty()) return;

        for (MobEntry entry : MobRegistry.getInstance().getMobs())
        {
            LivingEntity entity = entry.getEntity();
            if (entity == null)
            {
                LOGGER.warn("Skipping a JER mob entry whose entity supplier returned null");
                continue;
            }
            ResourceLocation table = entity instanceof Mob mob ? mob.getLootTable() : entity.getType().getDefaultLootTable();
            Function<String, Match> entityType = type -> {
                if (type.startsWith("#"))
                {
                    TagKey<net.minecraft.world.entity.EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE,
                        new ResourceLocation(type.substring(1)));
                    // Tags are not available in JER's offline world until a server has supplied them.
                    if (Minecraft.getInstance().level == null) return Match.CONDITIONAL;
                    return entity.getType().is(tag) ? Match.ALWAYS : Match.NEVER;
                }
                return type.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString())
                    ? Match.ALWAYS : Match.NEVER;
            };
            List<LootDrop> drops = transform(entry.getDrops(), modifiers, table, Match.ALWAYS, entityType);
            if (drops != entry.getDrops()) entry.setDrops(drops);
        }

        for (DungeonEntry entry : DungeonRegistry.getInstance().getDungeons())
        {
            String name = entry.getName();
            String path = DungeonRegistry.categoryToLocalKeyMap.entrySet().stream()
                .filter(mapping -> mapping.getValue().equals(name))
                .map(java.util.Map.Entry::getKey).findFirst().orElse(name);
            ResourceLocation table = ResourceLocation.tryParse(path);
            if (table == null || (!path.contains(":") && !path.startsWith("chests/")))
            {
                LOGGER.debug("Cannot determine loot table for custom JER dungeon {}", name);
                continue;
            }
            setChestDrops(entry, modifiers, table);
        }
    }

    private static List<Definition> loadModifiers()
    {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) return loadModifiers(server.getResourceManager());
        if (Settings.disableLootManagerReloading) return List.of();

        List<PackResources> packs = new ArrayList<>();
        packs.add(new ServerPacksSource().getVanillaPack());
        Services.PLATFORM.getModsList().getMods().forEach(mod -> packs.addAll(mod.getPackResources()));
        try (MultiPackResourceManager resources = new MultiPackResourceManager(PackType.SERVER_DATA, packs))
        {
            return loadModifiers(resources);
        }
    }

    private static List<Definition> loadModifiers(ResourceManager resources)
    {
        ResourceLocation listId = new ResourceLocation("forge", "loot_modifiers/global_loot_modifiers.json");
        JERModifierList list = new JERModifierList();
        for (var resource : resources.getResourceStack(listId))
        {
            try (var reader = resource.openAsReader())
            {
                list.addLayer(LootModifierManager.GSON_INSTANCE.fromJson(reader, JsonObject.class));
            }
            catch (IOException | com.google.gson.JsonParseException | IllegalStateException e)
            {
                LOGGER.error("Unable to read JER global loot modifier list from {}", resource.sourcePackId(), e);
            }
        }
        List<Definition> result = new ArrayList<>();
        for (String id : list.entries())
        {
            ResourceLocation location = new ResourceLocation(id);
            ResourceLocation file = new ResourceLocation(location.getNamespace(), "loot_modifiers/" + location.getPath() + ".json");
            var resource = resources.getResource(file);
            if (resource.isEmpty())
            {
                LOGGER.warn("Enabled global loot modifier {} has no resource for JER", location);
                continue;
            }
            try (var reader = resource.get().openAsReader())
            {
                JsonObject json = LootModifierManager.GSON_INSTANCE.fromJson(reader, JsonObject.class);
                if (json == null || !json.has("type"))
                    throw new com.google.gson.JsonSyntaxException("Global loot modifier requires a type");
                String type = GsonHelper.getAsString(json, "type");
                if (!type.equals("nfulib:add_item_for_entity") && !type.equals("nfulib:add_table")
                    && !type.equals("nfulib:replace_item")) continue;
                // Do not construct other mods' gameplay-only modifiers on the client.
                IGlobalLootModifier.DIRECT_CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.warn("Unable to decode NFU modifier {} for JER: {}", location, error))
                    .ifPresent(modifier -> IGlobalLootModifier.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, modifier)
                        .resultOrPartial(error -> LOGGER.warn("Unable to encode NFU modifier {} for JER: {}", location, error))
                        .ifPresent(encoded -> result.add(new Definition(modifier, encoded.getAsJsonObject()))));
            }
            catch (IOException | com.google.gson.JsonParseException | IllegalStateException e)
            {
                LOGGER.error("Unable to read global loot modifier {} for JER", location, e);
            }
        }
        return result;
    }

    private static List<LootDrop> transform(List<LootDrop> source, List<Definition> modifiers, ResourceLocation table,
        Match hasEntity, Function<String, Match> entityType)
    {
        List<LootDrop> drops = new ArrayList<>(source);
        boolean changed = false;
        for (Definition definition : modifiers)
        {
            JsonObject json = definition.json();
            JsonArray conditions = json.getAsJsonArray("conditions");
            Match match = JERLootConditions.all(conditions, table.toString(), hasEntity, entityType);
            if (match == Match.NEVER) continue;
            if (definition.modifier() instanceof AddItemForEntityLootModifier)
            {
                if (hasEntity == Match.NEVER) continue;
                Item addition = item(json, "addition");
                if (addition == null || addition == Items.BARRIER) continue;
                LootDrop drop = new LootDrop(addition.getDefaultInstance());
                if (json.has("functions"))
                {
                    LootItemFunction[] functions = LootModifierManager.GSON_INSTANCE.fromJson(json.get("functions"), LootItemFunction[].class);
                    drop.addLootFunctions(functions);
                }
                if (!drop.item.isEmpty())
                {
                    drops.add(drop);
                    changed = true;
                }
            }
            else if (definition.modifier() instanceof AddTableLootModifier)
            {
                ResourceLocation addition = new ResourceLocation(json.get("table").getAsString());
                // Parse the referenced table only; do not apply global modifiers to it again.
                for (LootDrop drop : LootTableHelper.toDrops(addition))
                {
                    if (drop.item.isEmpty()) continue;
                    drops.add(drop);
                    changed = true;
                }
            }
            else if (definition.modifier() instanceof ReplaceItemModifier)
            {
                Item original = item(json, "original");
                Item replacement = item(json, "replacement");
                if (original == null || original == Items.BARRIER || replacement == null || replacement == Items.BARRIER) continue;
                List<LootDrop> replaced = new ArrayList<>();
                for (LootDrop drop : drops)
                {
                    if (!drop.item.is(original))
                    {
                        replaced.add(drop);
                        continue;
                    }
                    if (match == Match.CONDITIONAL)
                        replaced.add(drop);
                    changed = true;
                    LootDrop newDrop = new LootDrop(new ItemStack(replacement, drop.item.getCount()),
                        drop.minDrop, drop.maxDrop, drop.chance, drop.fortuneLevel);
                    if (!newDrop.item.isEmpty())
                        replaced.add(newDrop);
                }
                drops = replaced;
            }
        }
        return changed ? drops : source;
    }

    private static Item item(JsonObject json, String key)
    {
        return json.has(key) ? ForgeRegistries.ITEMS.getValue(new ResourceLocation(json.get(key).getAsString())) : Items.BARRIER;
    }

    private static void setChestDrops(DungeonEntry entry, List<Definition> modifiers, ResourceLocation table)
    {
        // JER exposes no chest drop setter. Its field name is not Minecraft-obfuscated.
        try
        {
            Field field = DungeonEntry.class.getDeclaredField("drops");
            field.setAccessible(true);
            if (!(field.get(entry) instanceof Set<?> values))
                throw new IllegalStateException("Unexpected JER dungeon drops field type");
            List<LootDrop> drops = new ArrayList<>();
            for (Object value : values)
            {
                if (!(value instanceof LootDrop drop))
                    throw new IllegalStateException("Unexpected JER dungeon drop type");
                drops.add(drop);
            }
            // A chest context can contain its opening player, but need not contain any entity.
            List<LootDrop> transformed = transform(drops, modifiers, table, Match.CONDITIONAL, type ->
                type.startsWith("#") || type.equals("minecraft:player") ? Match.CONDITIONAL : Match.NEVER);
            if (transformed != drops) field.set(entry, new TreeSet<>(transformed));
        }
        catch (ReflectiveOperationException e)
        {
            LOGGER.error("Unable to update JER chest loot for {}", table, e);
        }
    }

    private record Definition(IGlobalLootModifier modifier, JsonObject json) {}
}
