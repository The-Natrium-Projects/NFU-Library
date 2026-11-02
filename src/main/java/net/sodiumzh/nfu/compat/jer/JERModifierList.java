package net.sodiumzh.nfu.compat.jer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class JERModifierList
{
    private final Set<String> entries = new LinkedHashSet<>();

    void addLayer(JsonObject layer)
    {
        if (layer == null || !layer.has("replace") || !layer.get("replace").isJsonPrimitive()
            || !layer.get("replace").getAsJsonPrimitive().isBoolean() || !layer.has("entries")
            || !layer.get("entries").isJsonArray())
            throw new JsonSyntaxException("Global loot modifier list requires replace and entries");
        boolean replace = layer.get("replace").getAsBoolean();
        List<String> additions = new ArrayList<>();
        for (JsonElement entry : layer.getAsJsonArray("entries"))
        {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString())
                throw new JsonSyntaxException("Global loot modifier entries must be resource location strings");
            additions.add(JERLootConditions.resourceId(entry.getAsString()));
        }
        if (replace) entries.clear();
        for (String entry : additions)
        {
            entries.remove(entry);
            entries.add(entry);
        }
    }

    List<String> entries()
    {
        return List.copyOf(entries);
    }
}
