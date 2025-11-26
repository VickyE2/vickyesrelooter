package io.github.vickye2.vickyesrelooter.data;

import com.google.gson.*;

import java.lang.reflect.Type;

public class LootableHolderSerializer implements JsonSerializer<LootableHolder> {

    @Override
    public JsonElement serialize(LootableHolder loot, Type typeOfSrc, JsonSerializationContext ctx) {
        JsonObject obj = new JsonObject();

        obj.addProperty("emptyWeight", loot.emptyWeight);
        obj.addProperty("tableWeight", loot.tableWeight);
        obj.addProperty("id", loot.id);

        if (loot.lootables != null && !loot.lootables.isEmpty()) {
            JsonArray itemsArray1 = new JsonArray();
            for (var item : loot.lootables) {
                itemsArray1.add(ctx.serialize(item));
            }
            obj.add("lootables", itemsArray1);
        }

        if (loot.singleLootables != null && !loot.singleLootables.isEmpty()) {
            JsonArray itemsArray2 = new JsonArray();
            for (var item : loot.singleLootables) {
                itemsArray2.add(ctx.serialize(item));
            }
            obj.add("singleLootables", itemsArray2);
        }

        return obj;
    }

    private static String toHex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
}
