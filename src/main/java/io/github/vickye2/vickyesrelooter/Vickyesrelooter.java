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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        Path tablesFolder = Paths.get(FMLPaths.CONFIGDIR.get().toString(), MODID, "tables");
        manager = new LootTableManager(new File(tablesFolder.toUri()));

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RelooterConfig.COMMON_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        event.enqueueWork(PacketHandler::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
            event.accept(TABLE_MAKER);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(manager));
        try {
            manager.load();
        } catch (IOException e) {
            LOGGER.info("Failed to start table manager");
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        LevelChunk chunk = (LevelChunk) event.getChunk();
        Level level = chunk.getLevel();
        RandomSource random = chunk.getLevel().getRandom();

        if (level.isClientSide) return;
        if (!event.isNewChunk() && !RelooterConfig.clearAlreadyLoadedChunks) return;

        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof ChestBlockEntity chest) {
                CompoundTag tag = chest.getPersistentData();

                if (!tag.getBoolean(LOOT_DATA_KEY)) {
                    applyCustomLoot(chest, random);
                }
            }
        }
    }

    private void applyCustomLoot(ChestBlockEntity chest, RandomSource random) {
        LootableHolder table = manager.chooseRandomTable(random);
        if (table == null) return;

        chest.clearContent();

        int size = chest.getContainerSize();

        for (int i = 0; i < size; i++) {
            LootableHolder.Lootable item = table.pickLoot(random);
            if (item == null) continue; // empty slot allowed

            ItemStack stack = item.createStack(random);
            if (!stack.isEmpty()) {
                chest.setItem(i, stack);
            }
        }

        chest.getPersistentData()
                .putBoolean(LOOT_DATA_KEY, true);
        chest.setChanged();
    }
}
