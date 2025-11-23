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

public record CreateTablePacket(String id, int emptyWeight, int tableWeight, List<LootableHolder.Lootable> lootables) {

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
    }

    public static CreateTablePacket decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int eW = buf.readInt();
        int tW = buf.readInt();

        int size = buf.readInt();
        var list = new ArrayList<LootableHolder.Lootable>();
        for (int i = 0; i < size; i++) {
            LootableHolder.Lootable able = new LootableHolder.Lootable();
            able.id = buf.readUtf();
            able.name = buf.readUtf();
            able.description = buf.readUtf();
            able.textColor = buf.readInt();
            able.descriptionColor = buf.readInt();
            able.weight = buf.readInt();
            able.minAmount = buf.readInt();
            able.maxAmount = buf.readInt();
            list.add(able);
        }

        return new CreateTablePacket(id, eW, tW, list);
    }


    public static void handle(CreateTablePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LootableHolder toMake = new LootableHolder();

            toMake.id = pkt.id;
            toMake.emptyWeight = pkt.emptyWeight;
            toMake.tableWeight = pkt.tableWeight;
            toMake.lootables = pkt.lootables;

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
