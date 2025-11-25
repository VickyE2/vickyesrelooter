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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.stream.Collectors;

public class LootTableManager {
    public static Thread tableWatcherThread;
    public static WatchService tableWatcher;
    public static Logger LOGGER = LoggerFactory.getLogger(LootTableManager.class);

    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LootableHolder.class, new LootableHolderSerializer())
            .registerTypeAdapter(LootableHolder.class, new LootableHolderDeserializer())
            .registerTypeAdapter(LootableHolder.Lootable.class, new LootableSerializer())
            .registerTypeAdapter(LootableHolder.Lootable.class, new LootableDeserializer())
            .setPrettyPrinting().create();

    private final File folder;
    private final Map<String, LootableHolder> tables = new HashMap<>();

    public LootTableManager(File configDir) {
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
        var names = new ArrayList<String>();
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                LootableHolder holder = gson.fromJson(reader, LootableHolder.class);
                names.add(holder.id);
                tables.put(file.getName(), holder);
            }
        }
        LOGGER.debug("Loaded table {}", String.join(", ", names));
    }

    public void startFileWatcher() throws IOException {
        if (tableWatcherThread != null) {
            tableWatcherThread.stop();
            tableWatcher.close();
        }

        load();

        Path tablesFolder = folder.toPath();
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
        emptyHolder.id = "extremely_default_table_table_smsm";

        File out = new File(folder, "default.json");
        try (FileWriter writer = new FileWriter(out)) {
            gson.toJson(emptyHolder, writer);
        }
    }

    public Map<String, LootableHolder> getTables() {
        return tables;
    }

    public LootableHolder chooseRandomTable(RandomSource r) {
        Collection<LootableHolder> tables = this.tables.values();
        int total = tables.stream().mapToInt(t -> t.tableWeight).sum();
        if (total == 0) {
            LOGGER.info("Tables is null or empty");
            return null;
        }

        int roll = r.nextInt(total);

        for (LootableHolder table : tables) {
            roll -= table.tableWeight;
            if (roll < 0) return table;
        }

        return null; // should never hit
    }
}

