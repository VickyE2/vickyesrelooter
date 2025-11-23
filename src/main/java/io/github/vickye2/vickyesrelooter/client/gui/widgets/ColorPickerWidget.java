package io.github.vickye2.vickyesrelooter.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ColorPickerWidget extends AbstractWidget {
    private final Minecraft mc = Minecraft.getInstance();
    private final SimpleSlider redSlider, greenSlider, blueSlider;
    private final Consumer<Integer> onChange;
    private SimpleSlider activeSlider = null;
    private final boolean showHex;
    private final Supplier<Integer> SUP_255 = () -> 255;

    private int red = 255, green = 255, blue = 255;

    public ColorPickerWidget(int x, int y, Consumer<Integer> onChange) {
        super(x, y, 0, 0, Component.literal("Color Picker"));

        int sliderWidth = 120;
        int sliderHeight = 20;
        int spacing = 25;
        showHex = false;
        this.onChange = onChange;

        redSlider = new SimpleSlider(x, y, sliderWidth, sliderHeight, "Red", red / 255.0, SUP_255, (value) -> {
            red = (int) (value * 255);
            onChange.accept(getColorInt());
        });
        greenSlider = new SimpleSlider(x, y + spacing, sliderWidth, sliderHeight, "Green", green / 255.0, SUP_255, (value) -> {
            green = (int) (value * 255);
            onChange.accept(getColorInt());
        });
        blueSlider = new SimpleSlider(x, y + spacing*2, sliderWidth, sliderHeight, "Blue", blue / 255.0, SUP_255, (value) -> {
            blue = (int) (value * 255);
            onChange.accept(getColorInt());
        });
    }

    public ColorPickerWidget(int x, int y, Consumer<Integer> onChange, boolean showHex) {
        super(x, y, 0, 0, Component.literal("Color Picker"));

        int sliderWidth = 120;
        int sliderHeight = 20;
        int spacing = 25;
        this.showHex = showHex;
        this.onChange = onChange;

        redSlider = new SimpleSlider(x, y, sliderWidth, sliderHeight, "Red", red / 255.0, SUP_255, (value) -> {
            red = (int) (value * 255);
            onChange.accept(getColorInt());
        });
        greenSlider = new SimpleSlider(x, y + spacing, sliderWidth, sliderHeight, "Green", green / 255.0, SUP_255, (value) -> {
            green = (int) (value * 255);
            onChange.accept(getColorInt());
        });
        blueSlider = new SimpleSlider(x, y + spacing*2, sliderWidth, sliderHeight, "Blue", blue / 255.0, SUP_255, (value) -> {
            blue = (int) (value * 255);
            onChange.accept(getColorInt());
        });
    }

    @Override
    public void render(@NotNull GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks) {
        redSlider.render(poseStack, mouseX, mouseY, partialTicks);
        greenSlider.render(poseStack, mouseX, mouseY, partialTicks);
        blueSlider.render(poseStack, mouseX, mouseY, partialTicks);

        // Draw preview box under sliders
        int previewX = getX() + 130;
        int previewY = getY();
        int previewW = 20;
        int previewH = 70;

        poseStack.fill(previewX, previewY, previewX + previewW, previewY + previewH, getColorInt());

        // Draw hex text
        if (showHex) {
            String hex = getHex();
            poseStack.drawString(mc.font, "Hex: " + hex, getX() + 5, previewY + 80, 0xFFFFFF);
        }
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int i, int i1, float v) {

    }

    public int getColorInt() {
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public String getHex() {
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (redSlider.mouseClicked(mouseX, mouseY, button)) {
            activeSlider = redSlider;
            return true;
        }
        if (greenSlider.mouseClicked(mouseX, mouseY, button)) {
            activeSlider = greenSlider;
            return true;
        }
        if (blueSlider.mouseClicked(mouseX, mouseY, button)) {
            activeSlider = blueSlider;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeSlider != null) {
            return activeSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (activeSlider != null) {
            boolean handled = activeSlider.mouseReleased(mouseX, mouseY, button);
            activeSlider = null;
            return handled;
        }
        return false;
    }

    public void setValue(int itemColor) {
        this.red = (itemColor >> 16) & 0xFF;
        this.green = (itemColor >> 8) & 0xFF;
        this.blue = itemColor & 0xFF;

        redSlider.setValue(red / 255.0);
        greenSlider.setValue(green / 255.0);
        blueSlider.setValue(blue / 255.0);

        if (onChange != null) onChange.accept(getColorInt());
    }
}
