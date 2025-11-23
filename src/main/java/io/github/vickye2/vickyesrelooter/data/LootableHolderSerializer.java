package io.github.vickye2.vickyesrelooter.data;

import com.google.gson.*;

import java.lang.reflect.Type;

public class LootableHolderSerializer implements JsonSerializer<LootableHolder> {

    @Override
    public JsonElement serialize(LootableHolder loot, Type typeOfSrc, JsonSerializationContext ctx) {
        JsonObject obj = new JsonObject();

        // Core fields
        obj.addProperty("emptyWeight", loot.emptyWeight);
        obj.addProperty("tableWeight", loot.tableWeight);
        obj.addProperty("id", loot.id);

        JsonArray itemsArray = new JsonArray();
        for (var item : loot.lootables) {
            itemsArray.add(ctx.serialize(item));
        }
        obj.add("lootables", itemsArray);

        return obj;
    }

    private static String toHex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
}
