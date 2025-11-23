package io.github.vickye2.vickyesrelooter.data;

import com.google.gson.*;
import java.lang.reflect.Type;

public class LootableDeserializer implements JsonDeserializer<LootableHolder.Lootable> {

    @Override
    public LootableHolder.Lootable deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();
        LootableHolder.Lootable loot = new LootableHolder.Lootable();

        loot.id = getString(obj, "id", "minecraft:stone");
        loot.name = getString(obj, "name", "");
        loot.description = getString(obj, "description", "");

        loot.weight = getInt(obj, "weight", 1);

        loot.textColor = parseColor(obj, "textColor", 0xFFFFFFFF);
        loot.descriptionColor = parseColor(obj, "descriptionColor", 0xFFFFFFFF);

        loot.minAmount = getInt(obj, "minAmount", 1);
        loot.maxAmount = getInt(obj, "maxAmount", loot.minAmount);

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
