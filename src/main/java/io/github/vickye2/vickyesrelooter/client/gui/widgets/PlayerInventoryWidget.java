package io.github.vickye2.vickyesrelooter.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * PlayerInventoryWidget - renders player's inventory grid (3 rows main + hotbar)
 * - calls onItemClicked with a copy of the clicked ItemStack
 * - shows vanilla item tooltips
 */
public class PlayerInventoryWidget extends AbstractWidget {

    private final Minecraft mc = Minecraft.getInstance();
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer =
            mc.getItemRenderer();
    private final Font font = mc.font;
    private final Consumer<ItemStack> onItemClicked;

    private final int cols = 9;
    private final int rows = 4; // 3 main + hotbar
    private final int slotSize = 18;

    public PlayerInventoryWidget(int x, int y, Consumer<ItemStack> onItemClicked) {
        super(x, y, 9 * 18, 4 * 18, Component.empty());
        this.onItemClicked = onItemClicked;
    }

    /**
     * Render widget contents (called by AbstractWidget.render(...) internals).
     */
    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (mc.player == null) return;

        var inventory = mc.player.getInventory();
        ItemStack hoveredStack = ItemStack.EMPTY;

        // Draw grid and items
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = (row == 3) ? col : 9 + row * 9 + col;
                ItemStack slotStack = inventory.getItem(index);

                int slotX = getX() + col * slotSize;
                int slotY = getY() + row * slotSize;

                // background (slot)
                graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF444444);

                if (slotStack != null && !slotStack.isEmpty() && !slotStack.is(Items.AIR)) {
                    graphics.renderItem(slotStack, slotX, slotY);
                    graphics.renderItemDecorations(font, slotStack, slotX, slotY);

                    // track hovered stack for tooltip rendering
                    if (mouseX >= slotX && mouseX <= slotX + 16 && mouseY >= slotY && mouseY <= slotY + 16) {
                        hoveredStack = slotStack;
                    }
                }
            }
        }

        // Render tooltip if hovering an item
        if (hoveredStack != null && !hoveredStack.isEmpty()) {
            // GuiGraphics exposes renderTooltip(Font, ItemStack, int, int) in 1.20.x
            graphics.renderTooltip(font, hoveredStack, mouseX, mouseY);
        }
    }

    /**
     * Handle clicks inside the widget. Returns true if handled.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mc.player == null) return false;

        var inventory = mc.player.getInventory();
        int mx = (int) mouseX;
        int my = (int) mouseY;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = (row == 3) ? col : 9 + row * 9 + col;

                int slotX = getX() + col * slotSize;
                int slotY = getY() + row * slotSize;

                if (mx >= slotX && mx <= slotX + 16 && my >= slotY && my <= slotY + 16) {
                    ItemStack clicked = inventory.getItem(index);
                    if (clicked != null && !clicked.isEmpty() && onItemClicked != null) {
                        onItemClicked.accept(clicked.copy());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // you can add narration text for accessibility if you'd like
    }
}
