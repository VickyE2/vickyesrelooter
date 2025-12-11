package io.github.vickye2.vickyesrelooter.network.packets;

import io.github.vickye2.vickyesrelooter.Vickyesrelooter;
import io.github.vickye2.vickyesrelooter.data.LootableHolder;
import io.github.vickye2.vickyesrelooter.manager.TableIO;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
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
            buf.writeUtf(stack.nbt);
            buf.writeUtf(stack.description);
            buf.writeInt(stack.textColor);
            buf.writeInt(stack.descriptionColor);
            buf.writeInt(stack.weight);
            buf.writeInt(stack.minAmount);
            buf.writeInt(stack.maxAmount);
            buf.writeBoolean(stack.isSureSpawn);
            buf.writeUtf(stack.sureSpawnGroup);
        }

        buf.writeInt(pkt.singleLootables.size());
        for (var stack : pkt.singleLootables) {
            buf.writeUtf(stack.id);
            buf.writeUtf(stack.name);
            buf.writeUtf(stack.nbt);
            buf.writeUtf(stack.description);
            buf.writeInt(stack.textColor);
            buf.writeInt(stack.descriptionColor);
            buf.writeInt(stack.weight);
            buf.writeInt(stack.minAmount);
            buf.writeInt(stack.maxAmount);
            buf.writeBoolean(stack.isSureSpawn);
            buf.writeUtf(stack.sureSpawnGroup);
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
            able.nbt = buf.readUtf();
            able.description = buf.readUtf();
            able.textColor = buf.readInt();
            able.descriptionColor = buf.readInt();
            able.weight = buf.readInt();
            able.minAmount = buf.readInt();
            able.maxAmount = buf.readInt();
            able.isSureSpawn = buf.readBoolean();
            able.sureSpawnGroup = buf.readUtf();
            list1.add(able);
        }

        int size2 = buf.readInt();
        var list2 = new ArrayList<LootableHolder.Lootable>();
        for (int i = 0; i < size2; i++) {
            LootableHolder.Lootable able = new LootableHolder.Lootable();
            able.id = buf.readUtf();
            able.name = buf.readUtf();
            able.nbt = buf.readUtf();
            able.description = buf.readUtf();
            able.textColor = buf.readInt();
            able.descriptionColor = buf.readInt();
            able.weight = buf.readInt();
            able.minAmount = buf.readInt();
            able.maxAmount = buf.readInt();
            able.isSureSpawn = buf.readBoolean();
            able.sureSpawnGroup = buf.readUtf();
            list2.add(able);
        }

        return new CreateTablePacket(id, eW, tW, list1, list2);
    }


    public static void handle(CreateTablePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        ctx.get().setPacketHandled(true); // always immediate

        // snapshot data because pkt & sender may not be safe to use later
        String id = pkt.id;
        int emptyWeight = pkt.emptyWeight;
        int tableWeight = pkt.tableWeight;
        List<LootableHolder.Lootable> lootables = new ArrayList<>(pkt.lootables);
        List<LootableHolder.Lootable> singleLootables = new ArrayList<>(pkt.singleLootables);

        // Offload to async worker
        TableIO.TABLE_EXECUTOR.submit(() -> {
            try {
                LootableHolder t = new LootableHolder();
                t.id = id;
                t.emptyWeight = emptyWeight;
                t.tableWeight = tableWeight;
                t.lootables = lootables;
                t.singleLootables = singleLootables;

                manager.addTable(t);   // JSON writing + map updates, FULLY off-thread

                // send result back on main thread
                if (sender != null) {
                    sender.getServer().execute(() ->
                            sender.sendSystemMessage(
                                    Component.literal("§aTable Created Successfully: §f" + id)
                            )
                    );
                }
            } catch (Exception e) {
                Vickyesrelooter.LOGGER.error("Table creation failed", e);

                if (sender != null) {
                    sender.getServer().execute(() ->
                            sender.sendSystemMessage(
                                    Component.literal("§cFailed to create table: " + e.getMessage())
                            )
                    );
                }
            }
        });
    }

}
