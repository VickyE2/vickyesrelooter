package io.github.vickye2.vickyesrelooter.network.packets;

import io.github.vickye2.vickyesrelooter.Vickyesrelooter;
import io.github.vickye2.vickyesrelooter.data.LootableHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static io.github.vickye2.vickyesrelooter.Vickyesrelooter.manager;

public record CreateTablePacket(String id, int emptyWeight, int tableWeight, List<LootableHolder.Lootable> lootables, List<LootableHolder.Lootable> singleLootables) {

    public static void encode(CreateTablePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.id);
        buf.writeInt(pkt.emptyWeight);
        buf.writeInt(pkt.tableWeight);

        buf.writeInt(pkt.lootables.size());
        for (var stack : pkt.lootables) {
            buf.writeUtf(stack.id);
            buf.writeUtf(stack.name);
            buf.writeUtf(stack.description);
            buf.writeInt(stack.textColor);
            buf.writeInt(stack.descriptionColor);
            buf.writeInt(stack.weight);
            buf.writeInt(stack.minAmount);
            buf.writeInt(stack.maxAmount);
        }

        buf.writeInt(pkt.singleLootables.size());
        for (var stack : pkt.singleLootables) {
            buf.writeUtf(stack.id);
            buf.writeUtf(stack.name);
            buf.writeUtf(stack.description);
            buf.writeInt(stack.textColor);
            buf.writeInt(stack.descriptionColor);
            buf.writeInt(stack.weight);
            buf.writeInt(stack.minAmount);
            buf.writeInt(stack.maxAmount);
        }
    }

    public static CreateTablePacket decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int eW = buf.readInt();
        int tW = buf.readInt();

        int size1 = buf.readInt();
        var list1 = new ArrayList<LootableHolder.Lootable>();
        for (int i = 0; i < size1; i++) {
            LootableHolder.Lootable able = new LootableHolder.Lootable();
            able.id = buf.readUtf();
            able.name = buf.readUtf();
            able.description = buf.readUtf();
            able.textColor = buf.readInt();
            able.descriptionColor = buf.readInt();
            able.weight = buf.readInt();
            able.minAmount = buf.readInt();
            able.maxAmount = buf.readInt();
            list1.add(able);
        }

        int size2 = buf.readInt();
        var list2 = new ArrayList<LootableHolder.Lootable>();
        for (int i = 0; i < size2; i++) {
            LootableHolder.Lootable able = new LootableHolder.Lootable();
            able.id = buf.readUtf();
            able.name = buf.readUtf();
            able.description = buf.readUtf();
            able.textColor = buf.readInt();
            able.descriptionColor = buf.readInt();
            able.weight = buf.readInt();
            able.minAmount = buf.readInt();
            able.maxAmount = buf.readInt();
            list2.add(able);
        }

        return new CreateTablePacket(id, eW, tW, list1, list2);
    }


    public static void handle(CreateTablePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LootableHolder toMake = new LootableHolder();

            toMake.id = pkt.id;
            toMake.emptyWeight = pkt.emptyWeight;
            toMake.tableWeight = pkt.tableWeight;
            toMake.lootables = pkt.lootables;
            toMake.singleLootables = pkt.singleLootables;

            try {
                manager.addTable(toMake);
                ctx.get().getSender().sendSystemMessage(Component.literal("§aTable Created Successfully"));
            } catch (IOException e) {
                Vickyesrelooter.LOGGER.error("Could not write table JSON!", e);
                ctx.get().getSender().sendSystemMessage(Component.literal("§sCould not write table JSON! " + e.getMessage()));
                e.printStackTrace();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
