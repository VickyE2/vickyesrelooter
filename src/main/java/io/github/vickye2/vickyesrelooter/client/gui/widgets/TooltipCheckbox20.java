package io.github.vickye2.vickyesrelooter.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class TooltipCheckbox20 extends Button {
    private boolean checked;
    private final Component tooltip;

    public TooltipCheckbox20(int x, int y, int size, Component label, Component tooltip, boolean initial, OnPress onPress) {
         super(x, y, size + 4 + Minecraft.getInstance().font.width(label), size, label, onPress, Supplier::get);
        this.checked = initial;
        this.tooltip = tooltip;
    }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean v) { checked = v; }

    @Override
    public void onPress() {
        checked = !checked;
        this.onPress.onPress(this);
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        Font font = Minecraft.getInstance().font;

        int boxSize = this.height - 4;
        int boxX = this.getX();
        int boxY = this.getY() + 2;

        // background for checkbox
        gui.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF1F1F1F); // dark background
        // border
        gui.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFFAAAAAA); // simple border

        // tick if checked
        if (checked) {
            // draw a simple tick with text - you can replace with sprite
            gui.drawString(font, Component.literal("✓"), boxX + (boxSize / 2) - 4, boxY + (boxSize / 2) - 8, 0xFF00FF00, false);
        }

        // label to the right of checkbox
        gui.drawString(font, this.getMessage(), boxX + boxSize + 4, this.getY() + (this.height - 8) / 2, 0xFFFFFFFF, false);

        // hover highlight
        if (this.isHoveredOrFocused()) {
            gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x33FFFFFF);
        }

        // tooltip when hovered
        if (this.isHoveredOrFocused()) {
            // GuiGraphics has a renderTooltip overload that accepts Component
            gui.renderTooltip(font, this.tooltip, mouseX, mouseY);
        }
    }
}