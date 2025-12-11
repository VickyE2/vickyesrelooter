package io.github.vickye2.vickyesrelooter.config;

import io.github.vickye2.vickyesrelooter.Vickyesrelooter;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Mod.EventBusSubscriber(modid = Vickyesrelooter.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RelooterConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
                .configure(CommonConfig::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    private static ForgeConfigSpec.ConfigValue<Boolean> cfgClearAlreadyLoadedChunks;
    private static ForgeConfigSpec.ConfigValue<Boolean> cfgApplyOnlyOnce;
    private static ForgeConfigSpec.ConfigValue<Boolean> cfgEnableRelooter;
    private static ForgeConfigSpec.ConfigValue<Boolean> cfgCanOverwritePlayerPlacedChests;
    private static ForgeConfigSpec.ConfigValue<Boolean> cfgAddTagToWrittenChests;
    private static ForgeConfigSpec.ConfigValue<Boolean> cfgDebugChunkLogging;
    private static ForgeConfigSpec.ConfigValue<String> cfgWrittenChestTag;
    private static ForgeConfigSpec.ConfigValue<Integer> cfgWrittenChestColor;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> cfgAllowedContainers;

    public static boolean clearAlreadyLoadedChunks;
    public static boolean applyOnlyOnce;
    public static boolean enableRelooter;
    public static boolean canOverwritePlayerPlacedChests;
    public static boolean addTagToWrittenChests;
    public static boolean debugChunkLogging;
    public static String writtenChestTag;
    public static int writtenChestColor;
    public static List<? extends String> allowedContainers;

    public static class CommonConfig {

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("General");
            cfgClearAlreadyLoadedChunks = builder
                    .comment("This set weather or not already loaded chunks (before mod was added. etc.) should be affected.")
                    .define("clearAlreadyLoadedChunks", true);
            cfgApplyOnlyOnce = builder
                    .comment("Once a chunk is affected it can't be affected again.")
                    .define("applyOnlyOnce", true);
            cfgEnableRelooter = builder
                    .comment("Weather or not the relooter logic should run.")
                    .define("enableRelooter", false);
            cfgCanOverwritePlayerPlacedChests = builder
                    .comment("Weather or not the relooter logic should run on chests that were placed by players.")
                    .define("canOverwritePlayerPlacedChests", false);
            cfgAddTagToWrittenChests = builder
                    .comment("Weather or not the relooter logic should add a tag to chests it ran on.")
                    .define("addTagToWrittenChests", true);
            cfgWrittenChestTag = builder
                    .comment("The string the relooter should use on logic ran chests.")
                    .define("writtenChestTag", "RELOOTED");
            cfgWrittenChestColor = builder
                    .comment("The color of the string the relooter should use on logic ran chests.")
                    .define("writtenChestColor", 0xd21546);
            cfgAllowedContainers = builder.comment("List of container blocks that the mod logic applies to.")
                    .defineList("allowedContainers",
                            List.of("minecraft:chest", "minecraft:barrel"),
                            o -> o instanceof String);
            builder.pop();
            builder.push("debug");
            cfgDebugChunkLogging = builder
                    .comment("Weather or not the relooter logic should add debug logs.")
                    .define("debugChunkLogging", false);
            builder.pop();
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        clearAlreadyLoadedChunks = cfgClearAlreadyLoadedChunks.get();
        applyOnlyOnce = cfgApplyOnlyOnce.get();
        enableRelooter = cfgEnableRelooter.get();
        canOverwritePlayerPlacedChests = cfgCanOverwritePlayerPlacedChests.get();
        addTagToWrittenChests = cfgAddTagToWrittenChests.get();
        writtenChestTag = cfgWrittenChestTag.get();
        writtenChestColor = cfgWrittenChestColor.get();
        allowedContainers = cfgAllowedContainers.get();
        debugChunkLogging = cfgDebugChunkLogging.get();
        AllowedContainerRegistry.refresh();
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        clearAlreadyLoadedChunks = cfgClearAlreadyLoadedChunks.get();
        applyOnlyOnce = cfgApplyOnlyOnce.get();
        enableRelooter = cfgEnableRelooter.get();
        canOverwritePlayerPlacedChests = cfgCanOverwritePlayerPlacedChests.get();
        addTagToWrittenChests = cfgAddTagToWrittenChests.get();
        writtenChestTag = cfgWrittenChestTag.get();
        writtenChestColor = cfgWrittenChestColor.get();
        allowedContainers = cfgAllowedContainers.get();
        debugChunkLogging = cfgDebugChunkLogging.get();
        AllowedContainerRegistry.refresh();
    }
}
