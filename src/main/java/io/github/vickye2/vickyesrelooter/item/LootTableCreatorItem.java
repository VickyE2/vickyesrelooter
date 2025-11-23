package io.github.vickye2.vickyesrelooter.item;

import io.github.vickye2.vickyesrelooter.client.gui.LootTableCreationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LootTableCreatorItem extends Item {
    public LootTableCreatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> LootTableCreatorItem::openScreen);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreen() {
        Minecraft.getInstance().setScreen(new LootTableCreationScreen());
    }

    // Always render glint
    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    // Custom tooltip
    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal("§a✦ Opens the Loot Table Creator ✦"));
        tooltip.add(Component.literal("§7Create & configure new tables easily..."));
        tooltip.add(Component.literal("Though.....the gui looks better on scale 2 for 1080p n 720p screens"));
    }
}
