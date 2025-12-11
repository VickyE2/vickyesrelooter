package io.github.vickye2.vickyesrelooter.client.gui.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class SimpleSlider extends AbstractSliderButton {
    private final String label;
    private final Supplier<Double> min;
    private final Supplier<Double> max;
    private final ValueChanged onValueChanged;

    public interface ValueChanged {
        void onChange(double newValue);
    }

    public SimpleSlider(int x, int y, int width, int height,
                        String label, double initialValue, Supplier<Double> min, Supplier<Double> max,
                        ValueChanged onValueChanged) {

        super(
                x, y, width, height,
                Component.empty(),
                (initialValue - min.get()) / (double) (max.get() - min.get())
        );

        this.label = label;
        this.min = min;
        this.max = max;
        this.onValueChanged = onValueChanged;

        updateMessage();
    }

    public SimpleSlider(int x, int y, int width, int height,
                        String label, double initialValue, Supplier<Integer> min, Supplier<Integer> max,
                        ValueChanged onValueChanged, int ignored) {

        super(
                x, y, width, height,
                Component.empty(),
                (initialValue - min.get()) / (double) (max.get() - min.get())
        );

        this.label = label;
        this.min = () -> (double) min.get();
        this.max = () -> (double) max.get();
        this.onValueChanged = onValueChanged;

        updateMessage();
    }

    @Override
    protected void updateMessage() {
        double actual = min.get() + (value * (max.get() - min.get()));
        setMessage(Component.literal(label + ": " + (int) (actual)));
    }

    @Override
    protected void applyValue() {
        double actual = min.get() + (double) (value * (max.get() - min.get()));
        onValueChanged.onChange(actual);
    }

    public void setActualValue(int v) {
        this.value = (v - min.get()) / (double) (max.get() - min.get());
        updateMessage();
    }

    public void setValue(double value) {
        this.value = value;
    }
}