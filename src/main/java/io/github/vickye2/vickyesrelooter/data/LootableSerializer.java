package io.github.vickye2.vickyesrelooter.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class LootableSerializer implements JsonSerializer<LootableHolder.Lootable> {

    @Override
    public JsonElement serialize(LootableHolder.Lootable loot, Type typeOfSrc, JsonSerializationContext ctx) {
        JsonObject obj = new JsonObject();

        // Core fields
        obj.addProperty("id", loot.id);
        obj.addProperty("name", loot.name);
        obj.addProperty("nbt", loot.nbt);
        obj.addProperty("description", loot.description);

        // Colors -> hex string
        obj.addProperty("textColor", toHex(loot.textColor));
        obj.addProperty("descriptionColor", toHex(loot.descriptionColor));

        obj.addProperty("weight", loot.weight);

        obj.addProperty("minAmount", loot.minAmount);
        obj.addProperty("maxAmount", loot.maxAmount);

        obj.addProperty("isSureSpawn", loot.isSureSpawn);
        obj.addProperty("sureSpawnGroup", loot.sureSpawnGroup);

        return obj;
    }

    private static String toHex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
}
