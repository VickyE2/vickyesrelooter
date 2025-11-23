package io.github.vickye2.vickyesrelooter.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class IntegerPickerWidget extends AbstractWidget {
    private final Minecraft mc = Minecraft.getInstance();
    private final SimpleSlider slider;

    private final Supplier<Integer> min;
    private final Supplier<Integer> max;

    private int value;
    private SimpleSlider activeSlider = null;

    public IntegerPickerWidget(int x, int y, Supplier<Integer> min, Supplier<Integer> max, Consumer<Integer> onChange) {
        super(x, y, 140, 40, Component.literal("Integer Picker"));

        this.min = min;
        this.max = max;
        this.value = min.get();

        // normalized initial slider value
        double sliderVal = (this.min.get()) / (double)(this.max.get() - this.min.get());

        slider = new SimpleSlider(
                x,
                y,
                120,
                20,
                "Value",
                sliderVal,
                max,
                (res) -> {
                    value = this.min.get() + (int)(res * (this.max.get() - this.min.get()));
                    onChange.accept(value);
                }
        );
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        slider.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (slider.mouseClicked(mouseX, mouseY, button)) {
            activeSlider = slider;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return activeSlider != null && activeSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (activeSlider != null) {
            boolean res = activeSlider.mouseReleased(mouseX, mouseY, button);
            activeSlider = null;
            return res;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {}

    public void setValue(int count) {
        this.value = count;
    }
}
