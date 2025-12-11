package io.github.vickye2.vickyesrelooter;


import com.mojang.logging.LogUtils;
import io.github.vickye2.vickyesrelooter.config.AllowedContainerRegistry;
import io.github.vickye2.vickyesrelooter.config.RelooterConfig;
import io.github.vickye2.vickyesrelooter.item.LootTableCreatorItem;
import io.github.vickye2.vickyesrelooter.manager.LootTableManager;
import io.github.vickye2.vickyesrelooter.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static io.github.vickye2.vickyesrelooter.config.RelooterConfig.canOverwritePlayerPlacedChests;
import static io.github.vickye2.vickyesrelooter.config.RelooterConfig.enableRelooter;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Vickyesrelooter.MODID)
public class Vickyesrelooter {
    public static final String MODID = "vickyesrelooter";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String LOOT_DATA_KEY = "CustomLootApplied";
    public static final String IS_PLAYER_PLACED_DATA_KEY = "ThisBlockWasPlacedByAPlayer";
    public static final String IS_ALREADY_TAGGED_DATA_KEY = "ThisBlockWasAlreadyTaggedSoNoMore";
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
    public static final Queue<RandomizableContainerBlockEntity> toLoot =
            new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public void onPlayerPlaceRCBE(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled()) {
            BlockPos pos = event.getPos();
            Block block = event.getPlacedBlock().getBlock();
            Level level = (Level) event.getLevel();
            BlockEntity be = level.getBlockEntity(pos);

            if (event.getPlacedBlock().hasBlockEntity()) {
                if (AllowedContainerRegistry.isNotAllowed(block))
                    return;

                if (be instanceof RandomizableContainerBlockEntity chest) {
                    chest.getPersistentData().putBoolean(IS_PLAYER_PLACED_DATA_KEY,
                            true);
                    chest.setChanged();
                }
            }
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!enableRelooter || event.getLevel().isClientSide()) return;

        LevelChunk chunk = (LevelChunk) event.getChunk();

        // Skip non-new chunks if not clearing
        if (!event.isNewChunk() && !RelooterConfig.clearAlreadyLoadedChunks) return;

        // Only store positions to loot asynchronously later
        chunk.getBlockEntities().forEach((pos, be) -> {
            Block block = chunk.getBlockState(pos).getBlock();
            if (AllowedContainerRegistry.isNotAllowed(block)) return;
            if (!(be instanceof RandomizableContainerBlockEntity rbe)) return;

            boolean playerPlaced = rbe.getPersistentData().getBoolean(IS_PLAYER_PLACED_DATA_KEY);
            if (playerPlaced && !canOverwritePlayerPlacedChests) return;

            // Only queue the reference; do not process loot here!
            toLoot.add(rbe);

            if (RelooterConfig.debugChunkLogging) {
                Vickyesrelooter.LOGGER.debug("[RelooterDebug] Queued RCBE at {}", pos);
            }
        });

        if (RelooterConfig.debugChunkLogging) {
            Vickyesrelooter.LOGGER.debug("[RelooterDebug] Chunk scan queued: {}", chunk.getPos());
        }
    }
}
