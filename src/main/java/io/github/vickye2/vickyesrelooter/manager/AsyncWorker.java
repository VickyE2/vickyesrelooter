package io.github.vickye2.vickyesrelooter.manager;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.vickye2.vickyesrelooter.data.LootableHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.vickye2.vickyesrelooter.Vickyesrelooter.*;
import static io.github.vickye2.vickyesrelooter.config.RelooterConfig.*;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AsyncWorker {
    public static final class SerializedStack {
        public final String itemId;
        public final int count;
        public final @Nullable String nbtJson;
        public final @Nullable String displayNameJson;
        public final @Nullable String loreJson;

        public SerializedStack(String itemId, int count, @Nullable String nbtJson,
                               @Nullable String displayNameJson, @Nullable String loreJson) {
            this.itemId = itemId;
            this.count = count;
            this.nbtJson = nbtJson;
            this.displayNameJson = displayNameJson;
            this.loreJson = loreJson;
        }
    }

    private static ExecutorService lootExecutor =
            Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
                    r -> {
                        Thread t = new Thread(r, "relooter-loot-worker");
                        t.setDaemon(true);
                        return t;
                    });

    private static final AtomicInteger runningBackgroundTasks = new AtomicInteger(0);
    private static final int MAX_BACKGROUND_TASKS = 8;

    private static ExecutorService getLootExecutor() {
        if (lootExecutor.isShutdown() || lootExecutor.isTerminated()) {
            lootExecutor = Executors.newFixedThreadPool(
                    Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
                    r -> {
                        Thread t = new Thread(r, "relooter-loot-worker");
                        t.setDaemon(true);
                        return t;
                    }
            );
        }
        return lootExecutor;
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (lootExecutor != null && !lootExecutor.isShutdown()) {
            lootExecutor.shutdownNow();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // pull up to N entities and submit them for background processing
        for (int i = 0; i < 10; i++) {
            if (runningBackgroundTasks.get() >= MAX_BACKGROUND_TASKS) break; // throttle
            RandomizableContainerBlockEntity entity = toLoot.poll();
            if (entity == null) break;

            // very small, safe pre-checks on main thread
            if (entity.isRemoved() || !entity.hasLevel()) continue;

            // snapshot needed identifiers (do NOT hold references to entity across threads)
            var pos = entity.getBlockPos();
            var dimension = entity.getLevel().dimension(); // ResourceKey<Level>
            int size = entity.getContainerSize();
            boolean alreadyTagged = entity.getPersistentData().getBoolean(LOOT_DATA_KEY);
            if (alreadyTagged && applyOnlyOnce)
                continue;

            // submit background work
            runningBackgroundTasks.incrementAndGet();
            getLootExecutor().submit(() -> {
                try {
                    List<SerializedStack> serialized = produceSerializedLootFor(pos, size);
                    if (serialized == null || serialized.isEmpty()) {
                        return;
                    }

                    // schedule actual application on the server thread
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server == null) return;

                    server.execute(() -> {
                        try {
                            applySerializedLootToEntityOnMainThread(server, dimension, pos, serialized);
                        } catch (Exception ex) {
                            LOGGER.error("Failed to apply serialized loot", ex);
                        }
                    });
                } finally {
                    runningBackgroundTasks.decrementAndGet();
                }
            });
        }
    }

    private static List<SerializedStack> produceSerializedLootFor(BlockPos pos,
                                                                  int size) {
        // pick table (this might do file IO or parsing — good to do off thread)
        RandomSource rnd = RandomSource.create(); // or new XoroshiroRandomSource(seed) — use deterministic if you need
        LootableHolder table = manager.chooseRandomTable(rnd); // ensure manager is thread-safe for this call
        if (table == null) return List.of();

        // replicate pick logic: pickGuaranteedLoot + pickWeighted etc
        // For brevity: assume table can give you Lootable objects and you read their id, amount, nbt string.
        List<SerializedStack> out = new ArrayList<>();
        try {
            // guaranteed ones:
            for (LootableHolder.Lootable g : table.getGuaranteedLoot(true)) {
                // get item id & count & raw NBT string representation
                String itemId = g.id;
                int count = g.minAmount == g.maxAmount ? g.minAmount : Mth.nextInt(rnd, g.minAmount, g.maxAmount);
                String nbtJson = (g.nbt != null && !g.nbt.isEmpty()) ? g.nbt : null;
                String nameJson = (g.name != null) ? Component.Serializer.toJson(Component.literal(g.name).withStyle(Style.EMPTY.withColor(g.textColor))) : null;
                String loreJson = (g.description != null) ? Component.Serializer.toJson(Component.literal(g.description).withStyle(Style.EMPTY.withColor(g.descriptionColor))) : null;
                out.add(new SerializedStack(itemId, count, nbtJson, nameJson, loreJson));
            }
            // do your normal weighted picks (table.pickLoot(...) etc), add them to out similarly
            LootableHolder.Lootable picked = table.pickLoot(rnd, new ArrayList<>());
            if (picked != null) {
                String itemId = picked.id;
                int count = picked.minAmount == picked.maxAmount ? picked.minAmount : Mth.nextInt(rnd, picked.minAmount, picked.maxAmount);
                String nbtJson = (picked.nbt != null && picked.nbt.startsWith("{")) ? picked.nbt : null;
                String nameJson = (picked.name != null) ? Component.Serializer.toJson(Component.literal(picked.name).withStyle(Style.EMPTY.withColor(picked.textColor))) : null;
                String loreJson = (picked.description != null) ? Component.Serializer.toJson(Component.literal(picked.description).withStyle(Style.EMPTY.withColor(picked.descriptionColor))) : null;
                out.add(new SerializedStack(itemId, count, nbtJson, nameJson, loreJson));
            }
        } catch (Exception ex) {
            LOGGER.error("Error while producing serialized loot for {}:{}", pos, ex);
        }

        // final size trim: ensure we don't exceed container size
        if (out.size() > size) out = out.subList(0, size);
        return out;
    }

    private static void applySerializedLootToEntityOnMainThread(MinecraftServer server,
                                                                ResourceKey<Level> dimension,
                                                                BlockPos pos,
                                                                List<SerializedStack> serialized) {
        Level level = server.getLevel(dimension);
        if (level == null) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RandomizableContainerBlockEntity entity)) return;
        if (entity.isRemoved()) return;

        // double-check not overwriting player-placed chests if configured
        if (entity.getPersistentData().getBoolean(IS_PLAYER_PLACED_DATA_KEY) && !canOverwritePlayerPlacedChests) return;

        entity.clearContent();

        int containerSize = entity.getContainerSize();
        for (int slot = 0; slot < serialized.size() && slot < containerSize; slot++) {
            SerializedStack ss = serialized.get(slot);
            if (ss == null) continue;
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(ss.itemId));
            if (item == null) continue;

            ItemStack stack = new ItemStack(item, ss.count);
            try {
                if (ss.nbtJson != null) {
                    stack.setTag(net.minecraft.nbt.TagParser.parseTag(ss.nbtJson));
                }
            } catch (CommandSyntaxException e) {
                LOGGER.warn("Invalid NBT for {}: {}", ss.itemId, ss.nbtJson);
            }

            if (ss.displayNameJson != null) {
                try {
                    stack.getOrCreateTagElement("display")
                            .putString("Name", ss.displayNameJson);
                } catch (Exception ex) {
                    LOGGER.warn("Failed to set display JSON for {}", ss.itemId, ex);
                }
            }
            if (ss.loreJson != null) {
                stack.getOrCreateTagElement("display").putString("Lore", ss.loreJson);
            }

            entity.setItem(slot, stack);
        }

        entity.getPersistentData().putBoolean(LOOT_DATA_KEY, true);
        if (addTagToWrittenChests && !entity.getPersistentData().getBoolean(IS_ALREADY_TAGGED_DATA_KEY)) {
            Component old = entity.getCustomName();
            Component appended = Component.literal("[").withStyle(Style.EMPTY.withColor(0x444444))
                    .append(Component.literal(writtenChestTag).withStyle(Style.EMPTY.withColor(writtenChestColor)))
                    .append(Component.literal("]").withStyle(Style.EMPTY.withColor(0x444444)));
            Component newName = (old == null)
                    ? appended
                    : Component.empty().append(old).append(" ").append(appended);
            entity.setCustomName(newName);
            entity.getPersistentData().putBoolean(IS_ALREADY_TAGGED_DATA_KEY, true);
        }
        entity.setChanged();
    }

}
