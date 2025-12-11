package io.github.vickye2.vickyesrelooter.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

import static io.github.vickye2.vickyesrelooter.config.RelooterConfig.allowedContainers;

public class AllowedContainerRegistry {

    private static final Set<Block> ALLOWED = new HashSet<>();

    public static void refresh() {
        ALLOWED.clear();

        for (String id : allowedContainers) {
            ResourceLocation rl = ResourceLocation.parse(id);

            if (ForgeRegistries.BLOCKS.containsKey(rl)) {
                Block b = ForgeRegistries.BLOCKS.getValue(rl);
                ALLOWED.add(b);
            }
        }
    }

    public static boolean isNotAllowed(Block block) {
        return !ALLOWED.contains(block);
    }
}

