package io.github.vickye2.vickyesrelooter.data;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class LootableHolderDeserializer implements JsonDeserializer<LootableHolder> {

    @Override
    public LootableHolder deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        LootableHolder loot = new LootableHolder();

        loot.id = getString(obj, "id", UUID.randomUUID().toString());
        loot.emptyWeight = getInt(obj, "emptyWeight", 45);
        loot.tableWeight = getInt(obj, "tableWeight", new Random().nextInt(5, 10));

        JsonArray itemsArray = obj.has("lootables") ? obj.getAsJsonArray("lootables") : new JsonArray();

        List<LootableHolder.Lootable> items1 = new ArrayList<>();
        for (JsonElement el : itemsArray) {
            LootableHolder.Lootable item = ctx.deserialize(el, LootableHolder.Lootable.class);
            items1.add(item);
        }
        loot.lootables = items1;

        JsonArray itemsArray2 = obj.has("singleLootables") ? obj.getAsJsonArray("singleLootables") : new JsonArray();

        List<LootableHolder.Lootable> items2 = new ArrayList<>();
        for (JsonElement el : itemsArray2) {
            LootableHolder.Lootable item = ctx.deserialize(el, LootableHolder.Lootable.class);
            items2.add(item);
        }
        loot.singleLootables = items2;

        return loot;
    }

    private static String getString(JsonObject obj, String key, String def) {
        return obj.has(key) ? obj.get(key).getAsString() : def;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) ? obj.get(key).getAsInt() : def;
    }

    private static int parseColor(JsonObject obj, String key, int def) {
        if (!obj.has(key)) return def;

        String hex = obj.get(key).getAsString();
        if (hex.startsWith("#")) hex = hex.substring(1);

        int rgb = Integer.parseInt(hex, 16);
        return 0xFF000000 | rgb; // force alpha to 0xFF
    }
}
