package io.github.vickye2.vickyesrelooter.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

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
        public String description;
        public int textColor;
        public int descriptionColor;
        public int weight;
        public int minAmount = 1;
        public int maxAmount = 1;

        @Override
        public String toString() {
            return "Lootable{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", textColor=" + textColor +
                    ", descriptionColor=" + descriptionColor +
                    ", weight=" + weight +
                    ", minAmount=" + minAmount +
                    ", maxAmount=" + maxAmount +
                    '}';
        }

        public ItemStack createStack(RandomSource random) {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(this.id));
            if (item == null) return ItemStack.EMPTY;

            int amount =
                    (minAmount == maxAmount || minAmount > maxAmount) ? minAmount :
                            Mth.nextInt(random, this.minAmount, this.maxAmount);
            ItemStack stack = new ItemStack(item, amount);

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
            totalWeight += l.weight;
        }

        for (LootableHolder.Lootable l : this.singleLootables) {
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
}