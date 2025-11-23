package io.github.vickye2.vickyesrelooter.client.gui.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class SimpleSlider extends AbstractSliderButton {
    private final String label;
    private final ValueChanged onValueChanged;
    private final Supplier<Integer> maxValue;

    public interface ValueChanged {
        void onChange(double value);
    }

    public SimpleSlider(int x, int y, int width, int height, String label, double initialValue, Supplier<Integer> maxValue, ValueChanged onValueChanged) {
        super(x, y, width, height, Component.literal(label + ": " + (int)(initialValue * maxValue.get())), initialValue);
        this.maxValue = maxValue;
        this.label = label;
        this.onValueChanged = onValueChanged;
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(label + ": " + (int)(value * maxValue.get())));
    }

    @Override
    protected void applyValue() {
        onValueChanged.onChange(value);
    }

    public void setValue(double value) {
        this.value = value;
    }
}
