package io.github.vickye2.vickyesrelooter.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.github.vickye2.vickyesrelooter.Vickyesrelooter;
import io.github.vickye2.vickyesrelooter.data.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class LootTableManager extends SimpleJsonResourceReloadListener {
    public static Thread tableWatcherThread;
    public static WatchService tableWatcher;

    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LootableHolder.class, new LootableHolderSerializer())
            .registerTypeAdapter(LootableHolder.class, new LootableHolderDeserializer())
            .registerTypeAdapter(LootableHolder.Lootable.class, new LootableSerializer())
            .registerTypeAdapter(LootableHolder.Lootable.class, new LootableDeserializer())
            .setPrettyPrinting().create();

    private final File folder;
    private final Map<String, LootableHolder> tables = new HashMap<>();

    public LootTableManager(File configDir) {
        super(gson, "tables");
        this.folder = new File(configDir, "loot_tables");
    }

    public void load() throws IOException {
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

        // If nothing exists, create a default empty table
        if (files == null || files.length == 0) {
            createDefaultTable();
            files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        }

        // Load them
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                LootableHolder holder = gson.fromJson(reader, LootableHolder.class);
                tables.put(file.getName(), holder);
            }
        }
    }

    public void startFileWatcher() throws IOException {
        if (tableWatcherThread != null) {
            tableWatcherThread.stop();
            tableWatcher.close();
        }

        Path tablesFolder = Paths.get(FMLPaths.CONFIGDIR.get().toString(), Vickyesrelooter.MODID, "tables");
        tableWatcher = FileSystems.getDefault().newWatchService();
        tablesFolder.register(tableWatcher, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);

        tableWatcherThread = new Thread(() -> {
            try {
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                AtomicReference<ScheduledFuture<?>> pendingReload = new AtomicReference<>();

                while (true) {
                    WatchKey key = tableWatcher.poll(500, TimeUnit.MILLISECONDS);
                    if (key == null) continue;

                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (pendingReload.get() != null) {
                            pendingReload.get().cancel(false);
                        }
                        pendingReload.set(
                                scheduler.schedule(() -> {
                                    try {
                                        this.load();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }, 300, TimeUnit.MILLISECONDS)
                        );
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        tableWatcherThread.start();
    }

    public void addTable(LootableHolder holder) throws IOException {
        if (!folder.exists()) {
            folder.mkdirs();
        }

        var json = gson.toJson(holder, LootableHolder.class);
        File newJson = new File(folder, holder.id + "_generated.json");
        if (newJson.exists()) {
            holder.id = holder.id + UUID.randomUUID();
            newJson = new File(folder, holder.id + "_generated.json");
        }

        try (FileWriter writer = new FileWriter(newJson)) {
            writer.write(json);
        }
        tables.put(holder.id, holder);
    }

    private void createDefaultTable() throws IOException {
        LootableHolder emptyHolder = new LootableHolder();
        emptyHolder.lootables = new ArrayList<>();
        emptyHolder.tableWeight = 1;

        File out = new File(folder, "default.json");
        try (FileWriter writer = new FileWriter(out)) {
            gson.toJson(emptyHolder, writer);
        }
    }

    public Map<String, LootableHolder> getTables() {
        return tables;
    }

    public LootableHolder chooseRandomTable(RandomSource r) {
        int total = tables.values().stream().mapToInt(t -> t.tableWeight).sum();
        if (total == 0) return null;

        int roll = r.nextInt(total);

        for (LootableHolder table : tables.values()) {
            roll -= table.tableWeight;
            if (roll < 0) return table;
        }

        return null; // should never hit
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        Map<String, LootableHolder> newTables = new HashMap<>();

        jsonMap.forEach((id, json) -> {
            try {
                LootableHolder table = gson.fromJson(json, LootableHolder.class);
                if (table != null && table.id != null) {
                    newTables.put(table.id, table);
                }
            } catch (Exception e) {
                Vickyesrelooter.LOGGER.error("Failed to parse table {}", id, e);
            }
        });

        this.tables.clear();
        this.tables.putAll(newTables);

        Vickyesrelooter.LOGGER.info("Loaded {} tables!", tables.size());
    }
}

