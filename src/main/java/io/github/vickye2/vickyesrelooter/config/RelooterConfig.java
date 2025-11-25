package io.github.vickye2.vickyesrelooter.config;

import io.github.vickye2.vickyesrelooter.Vickyesrelooter;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.fml.common.Mod;

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

    private static ForgeConfigSpec.ConfigValue<Boolean> ClearAlreadyLoadedChunks;
    private static ForgeConfigSpec.ConfigValue<Boolean> ApplyOnlyOnce;
    private static ForgeConfigSpec.ConfigValue<Boolean> EnableRelooter;

    public static boolean clearAlreadyLoadedChunks;
    public static boolean applyOnlyOnce;
    public static boolean enableRelooter;

    public static class CommonConfig {

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("General");
            ClearAlreadyLoadedChunks = builder
                    .comment("This set weather or not already loaded chunks (before mod was added. etc.) should be affected.")
                    .define("clearAlreadyLoadedChunks", true);
            ApplyOnlyOnce = builder
                    .comment("Once a chunk is affected it can't be affected again.")
                    .define("applyOnlyOnce", true);
            EnableRelooter = builder
                    .comment("Weather or not the relooter logic should run.")
                    .define("enableRelooter", false);
            builder.pop();
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        clearAlreadyLoadedChunks = ClearAlreadyLoadedChunks.get();
        applyOnlyOnce = ApplyOnlyOnce.get();
        enableRelooter = EnableRelooter.get();
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        clearAlreadyLoadedChunks = ClearAlreadyLoadedChunks.get();
        applyOnlyOnce = ApplyOnlyOnce.get();
        enableRelooter = EnableRelooter.get();
    }
}
