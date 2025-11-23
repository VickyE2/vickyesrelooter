package io.github.vickye2.vickyesrelooter.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class ClickActionableItem extends AbstractWidget {
    private final Consumer<ClickActionableItem> onCLick;
    private final ItemStack itemStack;
    private final Minecraft mc = Minecraft.getInstance();

    public ClickActionableItem(int x, int y, ItemStack stack, Consumer<ClickActionableItem> onCLick) {
        super(x, y, 16, 16, Component.literal("Clickable Item"));
        this.onCLick = onCLick;
        this.itemStack = stack;
    }

    @Override
    public void onClick(double p_93634_, double p_93635_) {
        super.onClick(p_93634_, p_93635_);
        onCLick.accept(this);
    }

    @Override
    public boolean isMouseOver(double mx, double my) {
        return mx >= getX() && mx < getX() + width &&
                my >= getY() && my < getY() + height;
    }

    @Override
    protected void renderWidget(GuiGraphics stack, int mouseX, int mouseY, float v) {
        stack.renderItem(itemStack, getX(), getY());
        stack.renderItemDecorations(mc.font, itemStack, getX(), getY());

        if (mouseX >= getX() && mouseX <= getX() + 16 && mouseY >= getY() && mouseY <= getY() + 16) {
            stack.renderTooltip(mc.font, itemStack, mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
