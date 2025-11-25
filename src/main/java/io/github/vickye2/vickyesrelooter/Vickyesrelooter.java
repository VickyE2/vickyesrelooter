package io.github.vickye2.vickyesrelooter;


import io.github.vickye2.vickyesrelooter.config.RelooterConfig;
import com.mojang.logging.LogUtils;
import io.github.vickye2.vickyesrelooter.data.*;
import io.github.vickye2.vickyesrelooter.item.LootTableCreatorItem;
import io.github.vickye2.vickyesrelooter.manager.LootTableManager;
import io.github.vickye2.vickyesrelooter.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static io.github.vickye2.vickyesrelooter.config.RelooterConfig.applyOnlyOnce;
import static io.github.vickye2.vickyesrelooter.config.RelooterConfig.enableRelooter;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Vickyesrelooter.MODID)
public class Vickyesrelooter {
    public static final String MODID = "vickyesrelooter";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String LOOT_DATA_KEY = "CustomLootApplied";
    public static LootTableManager manager;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> TABLE_MAKER =
            ITEMS.register("loot_table_registrar", () ->
                    new LootTableCreatorItem(new Item.Properties().food(
                                    new FoodProperties.Builder()
                                            .alwaysEat().nutrition(1)
                                            .saturationMod(2f).build())
                    )
            );

    public Vickyesrelooter() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onLoadComplete);
        modEventBus.addListener(this::addCreative);

        ITEMS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        Path tablesFolder = Paths.get(FMLPaths.CONFIGDIR.get().toString(), MODID);
        manager = new LootTableManager(new File(tablesFolder.toUri()));

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RelooterConfig.COMMON_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        if (!enableRelooter) {
            LOGGER.warn("Relooter is disabled, you should probably enable it.");
        }

        event.enqueueWork(PacketHandler::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
            event.accept(TABLE_MAKER);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        try {
            manager.startFileWatcher();
        } catch (IOException e) {
            LOGGER.info("Failed to start table manager");
            throw new RuntimeException(e);
        }
    }

    int amount = 10;
    int added = 0;
    private final Queue<RandomizableContainerBlockEntity> toLoot =
            new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (enableRelooter) {
            LevelChunk chunk = (LevelChunk) event.getChunk();
            Level level = chunk.getLevel();
            RandomSource random = chunk.getLevel().getRandom();

            if (added < amount) {
                added++;
            }

            if (level.isClientSide) return;
            if (!event.isNewChunk() && !RelooterConfig.clearAlreadyLoadedChunks) return;

            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof ChestBlockEntity entity) {
                    toLoot.add(entity);
                }
                else if (be instanceof BarrelBlockEntity entity) {
                    toLoot.add(entity);
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (int i = 0; i < 5; i++) { // process 5 chests per tick
                RandomizableContainerBlockEntity entity = toLoot.poll();
                if (entity != null) applyCustomLoot(entity, entity.getLevel().getRandom());
            }
        }
    }

    private void applyCustomLoot(RandomizableContainerBlockEntity entity, RandomSource random) {
        if (entity.getPersistentData().getBoolean(LOOT_DATA_KEY) && applyOnlyOnce) return;

        var pos = entity.getBlockPos();
        LootableHolder table = manager.chooseRandomTable(random);
        if (table == null) return;
        LOGGER.info("Lootifying entity at {} {} {} with table {}", pos.getX(), pos.getY(), pos.getZ(), table.id);

        entity.clearContent();

        int size = entity.getContainerSize();
        var context = new ArrayList<LootableHolder.Lootable>();

        for (int i = 0; i < size; i++) {
            LootableHolder.Lootable item = table.pickLoot(random, context);
            if (item == null) continue; // empty slot allowed

            ItemStack stack = item.createStack(random);
            if (!stack.isEmpty()) {
                entity.setItem(i, stack);
            }
        }

        entity.getPersistentData()
                .putBoolean(LOOT_DATA_KEY, true);
        entity.setChanged();
    }
}
