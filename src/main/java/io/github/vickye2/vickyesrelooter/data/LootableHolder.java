package io.github.vickye2.vickyesrelooter.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class LootableHolder {
    public String id;
    public List<Lootable> lootables;
    public List<Lootable> singleLootables;
    public int emptyWeight = 45;
    public int tableWeight = 10;

    @Override
    public String toString() {
        return "LootableHolder{" +
                "id='" + id + '\'' +
                ", emptyWeight=" + emptyWeight +
                ", tableWeight=" + tableWeight +
                ", lootables=" + lootables +
                ", singleLootables=" + singleLootables +
                '}';
    }

    public static class Lootable {
        public String id;
        public String name;
        public String nbt;
        public String description;
        public int textColor;
        public int descriptionColor;
        public int weight;
        public int minAmount = 1;
        public int maxAmount = 1;
        public boolean isSureSpawn = false;
        public String sureSpawnGroup = UUID.randomUUID().toString();

        @Override
        public String toString() {
            return "Lootable{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", nbt='" + nbt + '\'' +
                    ", description='" + description + '\'' +
                    ", textColor=" + textColor +
                    ", descriptionColor=" + descriptionColor +
                    ", weight=" + weight +
                    ", minAmount=" + minAmount +
                    ", maxAmount=" + maxAmount +
                    ", isSureSpawn=" + isSureSpawn +
                    ", sureSpawnGroup=" + sureSpawnGroup +
                    '}';
        }

        public ItemStack createStack(RandomSource random) throws CommandSyntaxException {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(this.id));
            if (item == null) return ItemStack.EMPTY;

            int amount =
                    (minAmount == maxAmount || minAmount > maxAmount) ? minAmount :
                            Mth.nextInt(random, this.minAmount, this.maxAmount);
            ItemStack stack = new ItemStack(item, amount);

            if (nbt != null && !nbt.isEmpty() && nbt.startsWith("{"))
                stack.setTag(net.minecraft.nbt.TagParser.parseTag(nbt));

            CompoundTag display = stack.getOrCreateTagElement("display");

            if (this.name != null)
                display.putString("Name", Component.Serializer.toJson(
                        Component.literal(this.name).withStyle(Style.EMPTY.withColor(this.textColor))
                ));

            if (this.description != null)
                display.putString("Lore", Component.Serializer.toJson(
                        Component.literal(this.description).withStyle(Style.EMPTY.withColor(this.descriptionColor))
                ));

            return stack;
        }
    }

    public LootableHolder.Lootable pickLoot(RandomSource random, List<Lootable> ctxSelected) {
        int totalWeight = this.emptyWeight;

        for (LootableHolder.Lootable l : this.lootables) {
            if (l.isSureSpawn) continue;
            totalWeight += l.weight;
        }

        for (LootableHolder.Lootable l : this.singleLootables) {
            if (l.isSureSpawn) continue;
            totalWeight += l.weight;
        }

        int roll = random.nextInt(totalWeight);

        // Check empty chance first
        if (roll < this.emptyWeight) return null;
        roll -= this.emptyWeight;

        for (LootableHolder.Lootable l : this.lootables) {
            if (roll < l.weight) return l;
            roll -= l.weight;
        }

        for (LootableHolder.Lootable l : this.singleLootables) {
            if (roll < l.weight) {
                if (ctxSelected.contains(l)) {
                    roll -= l.weight;
                }
                else {
                    ctxSelected.add(l);
                    return l;
                }
            }
            roll -= l.weight;
        }

        return null;
    }

    public List<Lootable> getGuaranteedLoot(boolean isChest) {
        if (!isChest) return List.of();

        List<Lootable> out = new ArrayList<>();

        // 1) Collect map of groups
        Map<String, List<Lootable>> groups = new HashMap<>();

        for (Lootable l : lootables) collect(l, out, groups);
        for (Lootable l : singleLootables) collect(l, out, groups);

        // 2) For each group → pick a single winner by weight
        Random r = new Random();
        for (var entry : groups.entrySet()) {
            List<Lootable> groupLoot = entry.getValue();

            int total = groupLoot.stream().mapToInt(l -> l.weight).sum();
            int roll = r.nextInt(total);

            Lootable winner = null;
            int running = 0;
            for (Lootable l : groupLoot) {
                running += l.weight;
                if (roll < running) {
                    winner = l;
                    break;
                }
            }
            if (winner != null) out.add(winner);
        }

        return out;
    }

    private void collect(Lootable l, List<Lootable> out, Map<String, List<Lootable>> groups) {
        if (!l.isSureSpawn) return;

        if (l.sureSpawnGroup == null || l.sureSpawnGroup.isEmpty()) {
            // Free agents = always spawn
            out.add(l);
        } else {
            groups.computeIfAbsent(l.sureSpawnGroup, g -> new ArrayList<>()).add(l);
        }
    }

    public List<ItemStack> generateLoot(RandomSource random, boolean isChest, int maxSize) throws CommandSyntaxException {
        int currentSize = 0;

        List<ItemStack> result = new ArrayList<>();

        // STEP 1: Add guaranteed items if chest
        for (Lootable g : getGuaranteedLoot(isChest)) {
            result.add(g.createStack(random));
            currentSize++;

            if (currentSize == maxSize) {
                return result;
            }
        }

        // STEP 2: Normal weighted roll (your existing system)
        List<Lootable> ctx = new ArrayList<>();
        Lootable picked = pickLoot(random, ctx);

        if (picked != null) {
            result.add(picked.createStack(random));
            currentSize++;

            if (currentSize == maxSize) {
                return result;
            }
        }

        return result;
    }

}