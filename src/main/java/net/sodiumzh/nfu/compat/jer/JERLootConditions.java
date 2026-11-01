package net.sodiumzh.nfu.compat.jer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.function.Function;

final class JERLootConditions
{
    enum Match
    {
        ALWAYS, NEVER, CONDITIONAL;

        Match not()
        {
            return this == ALWAYS ? NEVER : this == NEVER ? ALWAYS : CONDITIONAL;
        }
    }

    private JERLootConditions() {}

    static Match all(JsonArray conditions, String table, Match hasEntity, Function<String, Match> entityType)
    {
        boolean conditional = false;
        for (JsonElement condition : conditions)
        {
            Match match = match(condition.getAsJsonObject(), table, hasEntity, entityType);
            if (match == Match.NEVER) return Match.NEVER;
            conditional |= match == Match.CONDITIONAL;
        }
        return conditional ? Match.CONDITIONAL : Match.ALWAYS;
    }

    static Match match(JsonObject condition, String table, Match hasEntity, Function<String, Match> entityType)
    {
        String type = resourceId(condition.get("condition").getAsString());
        return switch (type)
        {
            case "forge:loot_table_id" -> resourceId(condition.get("loot_table_id").getAsString()).equals(table)
                ? Match.ALWAYS : Match.NEVER;
            case "minecraft:all_of" -> all(condition.getAsJsonArray("terms"), table, hasEntity, entityType);
            case "minecraft:any_of", "minecraft:alternative" -> any(condition.getAsJsonArray("terms"), table, hasEntity, entityType);
            case "minecraft:inverted" -> match(condition.getAsJsonObject("term"), table, hasEntity, entityType).not();
            case "minecraft:entity_properties" -> entity(condition, hasEntity, entityType);
            case "minecraft:killed_by_player" -> hasEntity == Match.ALWAYS ? Match.CONDITIONAL : Match.NEVER;
            case "minecraft:block_state_property" -> Match.NEVER;
            case "minecraft:random_chance" -> {
                float chance = condition.get("chance").getAsFloat();
                yield chance <= 0 ? Match.NEVER : chance >= 1 ? Match.ALWAYS : Match.CONDITIONAL;
            }
            default -> Match.CONDITIONAL;
        };
    }

    private static Match any(JsonArray conditions, String table, Match hasEntity, Function<String, Match> entityType)
    {
        boolean conditional = false;
        for (JsonElement condition : conditions)
        {
            Match match = match(condition.getAsJsonObject(), table, hasEntity, entityType);
            if (match == Match.ALWAYS) return Match.ALWAYS;
            conditional |= match == Match.CONDITIONAL;
        }
        return conditional ? Match.CONDITIONAL : Match.NEVER;
    }

    private static Match entity(JsonObject condition, Match hasEntity, Function<String, Match> entityType)
    {
        if (!condition.get("entity").getAsString().equals("this")) return Match.CONDITIONAL;
        JsonElement element = condition.get("predicate");
        // An absent predicate is EntityPredicate.ANY, which also accepts a null entity.
        if (element == null || element.isJsonNull()) return Match.ALWAYS;
        if (hasEntity == Match.NEVER) return Match.NEVER;
        JsonObject predicate = element.getAsJsonObject();
        Match type = hasEntity;
        if (predicate.has("type") && !predicate.get("type").isJsonNull())
        {
            String id = predicate.get("type").getAsString();
            type = entityType.apply(id.startsWith("#") ? "#" + resourceId(id.substring(1)) : resourceId(id));
        }
        if (type == Match.NEVER) return type;
        for (var field : predicate.entrySet())
            if (!field.getKey().equals("type") && hasConstraint(field.getValue()))
                return Match.CONDITIONAL;
        return type;
    }

    private static boolean hasConstraint(JsonElement element)
    {
        if (element.isJsonNull()) return false;
        if (element.isJsonObject())
            return element.getAsJsonObject().entrySet().stream().anyMatch(entry -> hasConstraint(entry.getValue()));
        if (element.isJsonArray())
            return !element.getAsJsonArray().isEmpty();
        return true;
    }

    static String resourceId(String id)
    {
        int separator = id.indexOf(':');
        String namespace = separator < 1 ? "minecraft" : id.substring(0, separator);
        String path = separator < 0 ? id : id.substring(separator + 1);
        if (!namespace.matches("[a-z0-9_.-]+") || !path.matches("[a-z0-9/._-]*"))
            throw new com.google.gson.JsonSyntaxException("Invalid resource location: " + id);
        return namespace + ":" + path;
    }
}
